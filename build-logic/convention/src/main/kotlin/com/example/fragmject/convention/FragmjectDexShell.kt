package com.example.fragmject.convention

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.File
import java.util.Properties
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * DEX 壳打包（H6 / S1 最小链路）。
 *
 * ============================ 做什么 ============================
 * 对一个已签名的 release APK 做后处理：
 * 1. 抽出全部 `classes*.dex`，逐块 XOR 混淆后写入 `assets/payload/dex-<n>.bin`；
 * 2. 删除原 dex，塞入壳模块编译出的 `classes.dex`（[StubApplication]）；
 * 3. 重新对齐、用发布证书重签（与 H1 的签名校验、H4 的重签流程一致）。
 *
 * manifest 的 `android:name` 不需要二进制改写：`:app` 使用 `${applicationName}` 占位符，
 * 开启壳时在构建期替换为 `com.example.shell.StubApplication`。
 *
 * 但 `android:appComponentFactory` **必须**改写：它由 `androidx.core:core` 的 manifest 注入，
 * 指向的类随业务 dex 一起进了载荷，而系统在 `LoadedApk.createAppFactory`（早于
 * `StubApplication.attachBaseContext`）就会按名加载 → 必然 `ClassNotFoundException`
 * （logcat：`Unable to instantiate appComponentFactory`）。剔除逻辑在 [ShellManifest]。
 *
 * ============================ 分工 ============================
 * 本文件只做**构建编排**：路径解析、任务注册、进程调用（d8 / zipalign / apksigner）、
 * 签名材料读取。载荷的字节层面协议（deflate → XOR、分片命名、`sum.txt` / `shell.txt`、
 * 旧签名剔除、gadget 资源）全部在 [ShellPayload]（纯逻辑，有单测）。
 *
 * ============================ 开关 ============================
 * `-PenableDexShell=true` 才会注册任务，同时才会把 manifest 占位符指向 stub。
 * 两者必须成对出现：占位符指向 stub 但没打壳，会得到一个启动即崩的 APK。
 */
internal fun Project.configureDexShell() {
    val enabled = providers.gradleProperty(PROP_ENABLE_DEX_SHELL).map(String::toBoolean).getOrElse(false)
    if (!enabled) return

    val config = FragmjectConfig.load(this)
    val sdkDirectory = extensions
        .getByType(com.android.build.api.variant.ApplicationAndroidComponentsExtension::class.java)
        .sdkComponents
        .sdkDirectory

    val baseDir = projectDir.absolutePath
    val shellProject = project(":shell")
    val shellAar = shellProject.layout.buildDirectory.file("outputs/aar/shell-release.aar")
    val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
    val shelledApk = layout.buildDirectory.file("outputs/apk/release/app-release-shell.apk")
    // 平台目录名带 minor（android-37.1）；用配置里的拼装结果，避免这里写死 major
    val platformJarName = config.compileSdkPlatformName

    val shellGenDir = layout.buildDirectory.dir("generated/shell")

    // 壳 dex 必须**先**产出：打包任务要把它原样塞进 APK，且 assets/payload/shell.txt
    // 里的 CRC 必须与这次产出的字节一一对应。顺序错了就变成「校验一个还没生成的 dex」。
    val generateTask = tasks.register<GenerateShellDexTask>("generateShellDex") {
        group = "build"
        description = "d8 编译壳 dex（供 packShellRelease 塞进 APK）"
        shellAarFile.set(shellAar)
        sdkDir.set(sdkDirectory)
        platformJarDirName.set(platformJarName)
        outputDir.set(shellGenDir)
        dependsOn(shellProject.tasks.named("assembleRelease"))
    }

    // 与资源混淆叠加时**必须串行**：资源混淆会重排 zip 并重签，若两者并行处理
    // 同一个 app-release.apk，会产出「manifest 指向壳类但没有壳 dex」的坏包（实测）。
    // 因此开壳时优先消费 resobf 产物，并在任务图上显式依赖它。
    val resObfEnabled = providers.gradleProperty("enableResObf").map(String::toBoolean).getOrElse(false)
    val obfApk = layout.buildDirectory.file("outputs/apk/release/app-release-resobf.apk")

    // 只有显式开启探针时才给 gadgetDir 赋值：`@InputDirectory` 上的 `@Optional` 只表示
    // 「属性可以未设置」，一旦**设置成不存在的目录**，任务仍在输入校验阶段直接失败
    // （CI 上 `tools/frida-gadget` 是 gitignore 的，weekly release 就因此挂掉）。
    // 未开启时不赋值 → `gadgetDir.orNull == null`，而任务侧只在 `gadgetProbe=true` 时才读它。
    val gadgetProbeEnabled = providers.gradleProperty("enableGadgetProbe").map(String::toBoolean).getOrElse(false)

    val packTask = tasks.register<PackShellTask>("packShellRelease") {
        group = "build"
        description = "把 release APK 打成 DEX 壳（H6/S1）"
        inputApk.set(if (resObfEnabled) obfApk else releaseApk)
        shellDexFile.set(shellGenDir.map { it.file("shell.dex") })
        sdkDir.set(sdkDirectory)
        signingPropertiesFile.set(rootProject.layout.projectDirectory.file("keystore.properties"))
        signingBaseDir.set(baseDir)
        outputApk.set(shelledApk)
        gadgetProbe.set(gadgetProbeEnabled)
        if (gadgetProbeEnabled) {
            gadgetDir.set(rootProject.layout.projectDirectory.dir("tools/frida-gadget"))
        }
        dependsOn(tasks.named(if (resObfEnabled) "obfuscateReleaseResources" else "packageRelease"))
        dependsOn(generateTask)
    }
    tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(packTask) }

    // 打壳前必须有壳 dex（pack 任务已显式依赖 generateTask）
}

/** 开关名：`-PenableDexShell=true`。 */
private const val PROP_ENABLE_DEX_SHELL = "enableDexShell"

/**
 * 打包任务本体：d8 生成壳 dex → 重排 zip（原 dex 换成加密载荷 + 壳 dex）→ 对齐 → 重签。
 */
abstract class PackShellTask : DefaultTask() {

    @get:InputFile
    abstract val inputApk: RegularFileProperty

    /** 由 [GenerateShellDexTask] 产出的壳 dex（与打进 APK 的字节完全一致，CRC 才对得上）。 */
    @get:InputFile
    abstract val shellDexFile: RegularFileProperty

    /** SDK 根目录（定位 zipalign / apksigner）。 */
    @get:InputDirectory
    abstract val sdkDir: DirectoryProperty

    @get:InputFile
    abstract val signingPropertiesFile: RegularFileProperty

    @get:Input
    abstract val signingBaseDir: Property<String>

    /** 是否打入 Frida Gadget 探针（`-PenableGadgetProbe=true`，仅测试包）。 */
    @get:Input
    abstract val gadgetProbe: Property<Boolean>

    /** 探针 so 所在目录（仓库内 `tools/frida-gadget`，不入库）。 */
    @get:InputDirectory
    @get:Optional
    abstract val gadgetDir: DirectoryProperty

    @get:OutputFile
    abstract val outputApk: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun run() {
        val work = temporaryDir.apply { deleteRecursively(); mkdirs() }
        val tools = resolveBuildTools(sdkDir.get().asFile, "d8 / zipalign / apksigner")
        val shellDex = shellDexFile.get().asFile
        check(shellDex.exists()) { "缺少壳 dex：${shellDex.absolutePath}（先运行 generateShellDex）" }

        // 重排 zip：原 dex 换成加密载荷 + 壳 dex（字节层面全部在 ShellPayload）
        val rebuilt = File(work, "rebuilt.apk")
        val result = ShellPayload.repack(
            source = inputApk.get().asFile,
            target = rebuilt,
            shellDex = shellDex.readBytes(),
            gadget = resolveGadgetPayload(),
            warn = { message -> logger.warn(message) },
        )
        logger.lifecycle("载荷分片：${result.payloadChunks} 个")
        // 壳包 manifest 里指向载荷的 appComponentFactory 已剔除（见 ShellManifest）
        result.removedAppComponentFactory?.let {
            logger.lifecycle("已剔除 appComponentFactory（原值：$it）")
        }

        // 3. 对齐 + 重签
        val aligned = File(work, "aligned.apk")
        exec.exec {
            commandLine(
                File(tools, "zipalign").absolutePath, "-p", "-f", "4",
                rebuilt.absolutePath, aligned.absolutePath,
            )
        }
        val signed = File(work, "signed.apk")
        val signing = readSigningConfig()
        exec.exec {
            commandLine(
                File(tools, "apksigner").absolutePath, "sign",
                "--ks", signing.storeFile,
                "--ks-pass", "pass:${signing.storePassword}",
                "--ks-key-alias", signing.keyAlias,
                "--key-pass", "pass:${signing.keyPassword}",
                "--out", signed.absolutePath, aligned.absolutePath,
            )
            standardOutput = java.io.ByteArrayOutputStream()
            errorOutput = java.io.ByteArrayOutputStream()
        }

        outputApk.get().asFile.apply { parentFile?.mkdirs() }.also { signed.copyTo(it, overwrite = true) }
        logger.lifecycle("DEX 壳完成：${outputApk.get().asFile.absolutePath}")
    }

    /** gadget 探针（仅测试包）：把 so 与配置读成内存数据交给 [ShellPayload.repack]。 */
    private fun resolveGadgetPayload(): ShellPayload.GadgetPayload? {
        if (!gadgetProbe.get()) return null
        val dir = gadgetDir.orNull?.asFile
            ?: error("启用 gadget 探针但未提供 gadgetDir（默认 tools/frida-gadget）")
        val so = File(dir, "lib/arm64-v8a/libgadget.so")
        check(so.exists()) {
            "缺少 gadget：${so.absolutePath}；先下载 frida-gadget-<ver>-android-arm64.so 并改名"
        }
        // 配置里 on_load=resume：否则 gadget 会阻塞在「等待客户端连接」上
        val config = File(dir, "lib/arm64-v8a/libgadget.config.so")
        return ShellPayload.GadgetPayload(so.readBytes(), config.takeIf { it.exists() }?.readBytes())
    }

    private data class Signing(
        val storeFile: String,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String,
    )

    private fun readSigningConfig(): Signing {
        val file = signingPropertiesFile.get().asFile
        check(file.exists()) { "缺少 ${file.absolutePath}：打壳后必须用发布证书重签" }
        val props = Properties().apply { file.inputStream().use { load(it) } }
        val storePath = props.getProperty("storeFile") ?: error("keystore.properties 缺少 storeFile")
        val store = File(signingBaseDir.get()).resolve(storePath).canonicalFile
        check(store.exists()) { "找不到 keystore：${store.absolutePath}" }
        return Signing(
            storeFile = store.absolutePath,
            storePassword = props.getProperty("storePassword").orEmpty(),
            keyAlias = props.getProperty("keyAlias").orEmpty(),
            keyPassword = props.getProperty("keyPassword").orEmpty(),
        )
    }
}

/**
 * 生成壳 dex。
 *
 * 为什么单独一个任务：
 * - 打包任务要把它**原样**塞进 APK，`assets/payload/shell.txt` 里的 CRC 必须与这份字节对应；
 * - 顺序错了（先 pack 后 generate）就会得到一个「清单与 dex 不是同一次生成」的包，
 *   壳启动校验直接 fail-closed。
 *
 * 输出只有一个文件：`shell.dex`。
 *
 * 历史上的 `shell_crc.h`（把 CRC 作为编译期常量注入 native）已随 so 迁入 `:security`
 * 而废弃——库不该内置某个宿主 App 的壳 dex 摘要，改由 `assets/payload/shell.txt`
 * 承担同信任域校验（见 `docs/hardening-assessment.md` §9 与 `StubApplication.verifyShellDex`）。
 */
abstract class GenerateShellDexTask : DefaultTask() {

    @get:InputFile
    abstract val shellAarFile: RegularFileProperty

    /** SDK 根目录（定位 android.jar 与 build-tools）。 */
    @get:InputDirectory
    abstract val sdkDir: DirectoryProperty

    @get:Input
    abstract val platformJarDirName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun run() {
        val out = outputDir.get().asFile.apply { mkdirs() }
        val work = temporaryDir.apply { deleteRecursively(); mkdirs() }
        val tools = resolveBuildTools(sdkDir.get().asFile, "d8")
        val androidJar = resolveAndroidJar()

        val shellJar = File(work, "shell-classes.jar")
        extractClassesJar(shellAarFile.get().asFile, shellJar)

        val dexOut = File(work, "shell-dex").apply { mkdirs() }
        exec.exec {
            commandLine(
                File(tools, "d8").absolutePath,
                "--lib", androidJar.absolutePath,
                "--min-api", "26",
                "--output", dexOut.absolutePath,
                shellJar.absolutePath,
            )
            standardOutput = java.io.ByteArrayOutputStream()
            errorOutput = java.io.ByteArrayOutputStream()
        }

        val dex = File(dexOut, "classes.dex")
        check(dex.exists()) { "d8 未产出 classes.dex" }
        dex.copyTo(File(out, "shell.dex"), overwrite = true)

        // 壳 dex 的 CRC 由打包任务写进 assets/payload/shell.txt（壳自己校验）。
        // 刻意不再注入 native：so 已搬到可复用的 :security 库，
        // 库里不该内置「某个宿主 App 的壳 dex 摘要」。
        val crc = java.util.zip.CRC32().apply { update(dex.readBytes()) }.value
        logger.lifecycle("壳 dex ${dex.length()} B，CRC=0x%08x".format(crc))
    }

    private fun extractClassesJar(aar: File, target: File) {
        ZipFile(aar).use { zip ->
            val entry = zip.getEntry("classes.jar") ?: error("AAR 内没有 classes.jar：${aar.absolutePath}")
            zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
        }
    }

    /** 平台目录名不一定是 `android-<sdk>`（本地实测为 `android-37.0`），优先精确匹配再退回最大版本。 */
    private fun resolveAndroidJar(): File {
        val platforms = sdkDir.get().asFile.resolve("platforms").listFiles()
            ?.filter { it.isDirectory && File(it, "android.jar").exists() }
            .orEmpty()
        val exact = platforms.firstOrNull { it.name == "android-${platformJarDirName.get()}" }
        val chosen = exact ?: platforms.maxByOrNull { it.name.filter(Char::isDigit).toIntOrNull() ?: 0 }
        checkNotNull(chosen) { "找不到任何 android.jar（platforms 目录为空）" }
        return File(chosen, "android.jar")
    }
}

/**
 * 定位 SDK build-tools：取含 `d8` 的最高版本目录。
 *
 * 两个任务原本各自复制了一份，[requirement] 只用于还原各自的错误文案。
 */
private fun resolveBuildTools(sdkDir: File, requirement: String): File =
    sdkDir.resolve("build-tools").listFiles()
        ?.filter { it.isDirectory && File(it, "d8").exists() }
        ?.maxByOrNull { it.name }
        ?: error("未找到 build-tools（需要 $requirement）")
