package com.example.fragmject.convention

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
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
 * manifest 不需要二进制改写：`:app` 的 `android:name` 使用 `${applicationName}` 占位符，
 * 开启壳时在构建期替换为 `com.example.shell.StubApplication`。
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
    val platformJarName = "android-${config.compileSdk}"

    val packTask = tasks.register<PackShellTask>("packShellRelease") {
        group = "build"
        description = "把 release APK 打成 DEX 壳（H6/S1）"
        inputApk.set(releaseApk)
        shellAarFile.set(shellAar)
        sdkDir.set(sdkDirectory)
        platformJarDirName.set(platformJarName)
        signingPropertiesFile.set(rootProject.layout.projectDirectory.file("keystore.properties"))
        signingBaseDir.set(baseDir)
        outputApk.set(shelledApk)
        dependsOn(tasks.named("packageRelease"))
        dependsOn(shellProject.tasks.named("assembleRelease"))
    }
    tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(packTask) }
}

/** 开关名：`-PenableDexShell=true`。 */
private const val PROP_ENABLE_DEX_SHELL = "enableDexShell"

/**
 * 打包任务本体：d8 生成壳 dex → 重排 zip（原 dex 换成加密载荷 + 壳 dex）→ 对齐 → 重签。
 */
abstract class PackShellTask : DefaultTask() {

    @get:InputFile
    abstract val inputApk: RegularFileProperty

    @get:InputFile
    abstract val shellAarFile: RegularFileProperty

    /** SDK 根目录，用于定位 platforms/android-XX/android.jar 与 build-tools 下的工具。 */
    @get:InputDirectory
    abstract val sdkDir: DirectoryProperty

    @get:Input
    abstract val platformJarDirName: Property<String>

    @get:InputFile
    abstract val signingPropertiesFile: RegularFileProperty

    @get:Input
    abstract val signingBaseDir: Property<String>

    @get:OutputFile
    abstract val outputApk: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun run() {
        val work = temporaryDir.apply { deleteRecursively(); mkdirs() }
        val tools = resolveBuildTools()
        // 平台目录名不一定是 android-<sdk>（本地实测为 android-37.0），因此优先精确匹配，
        // 否则退回版本号最大的那个平台：d8 只需要一个 API 级别的 android.jar。
        val androidJar = resolveAndroidJar()
        logger.lifecycle("android.jar：${androidJar.absolutePath}")

        // 1. 壳 AAR → classes.jar → d8 → classes.dex
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
        val shellDex = File(dexOut, "classes.dex")
        check(shellDex.exists()) { "d8 未产出 classes.dex" }
        logger.lifecycle("壳 dex 大小：${shellDex.length()} B")

        // 2. 重排 zip：原 dex 换成加密载荷 + 壳 dex
        val rebuilt = File(work, "rebuilt.apk")
        val payloadCount = repack(inputApk.get().asFile, shellDex, rebuilt)
        logger.lifecycle("载荷分片：$payloadCount 个")

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

    /** 从 AAR 里取出 `classes.jar`。 */
    private fun extractClassesJar(aar: File, target: File) {
        ZipFile(aar).use { zip ->
            val entry = zip.getEntry("classes.jar") ?: error("AAR 内没有 classes.jar：${aar.absolutePath}")
            zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
        }
    }

    /** @return 载荷分片数量。 */
    private fun repack(source: File, shellDex: File, target: File): Int {
        ZipFile(source).use { zip ->
            val dexNames = zip.entries().asSequence()
                .map { it.name }
                .filter { DEX_NAME_REGEX.matches(it) }
                .toList()

            ZipOutputStream(FileOutputStream(target)).use { out ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name !in dexNames }
                    .filter { !isSignatureFile(it.name) }
                    .forEach { entry -> copyEntry(zip, entry, out) }

                dexNames.forEachIndexed { index, name ->
                    val raw = zip.getInputStream(zip.getEntry(name)).use { it.readBytes() }
                    writeEntry(out, "assets/payload/dex-$index.bin", encode(raw))
                }
                writeEntry(out, "classes.dex", shellDex.readBytes())
            }
            return dexNames.size
        }
    }

    private fun copyEntry(zip: ZipFile, entry: ZipEntry, out: ZipOutputStream) {
        val copy = ZipEntry(entry.name).apply {
            method = entry.method
            time = entry.time
            if (entry.method == ZipEntry.STORED) {
                size = entry.size
                crc = entry.crc
            }
        }
        out.putNextEntry(copy)
        zip.getInputStream(entry).use { it.copyTo(out) }
        out.closeEntry()
    }

    private fun writeEntry(out: ZipOutputStream, name: String, data: ByteArray) {
        out.putNextEntry(ZipEntry(name))
        out.write(data)
        out.closeEntry()
    }

    private fun isSignatureFile(name: String): Boolean {
        val upper = name.uppercase()
        return uppercaseSignaturePrefixes.any { upper.startsWith("META-INF/") && upper.endsWith(it) }
    }

    /** 与 [com.example.shell.StubApplication] 的 decode 对应。 */
    private fun encode(data: ByteArray): ByteArray = ByteArray(data.size) { i ->
        (data[i].toInt() xor (XOR_BASE + i)).toByte()
    }

    /** 定位 android.jar：优先 `android-<compileSdk>`，否则取版本号最大的平台。 */
    private fun resolveAndroidJar(): File {
        val platforms = sdkDir.get().asFile.resolve("platforms").listFiles()
            ?.filter { it.isDirectory && File(it, "android.jar").exists() }
            .orEmpty()
        val exact = platforms.firstOrNull { it.name == "android-${platformJarDirName.get()}" }
        val chosen = exact ?: platforms.maxByOrNull {
            it.name.filter(Char::isDigit).toIntOrNull() ?: 0
        }
        checkNotNull(chosen) { "找不到任何 android.jar（platforms 目录为空）" }
        return File(chosen, "android.jar")
    }

    private fun resolveBuildTools(): File =
        sdkDir.get().asFile.resolve("build-tools").listFiles()
            ?.filter { it.isDirectory && File(it, "d8").exists() }
            ?.maxByOrNull { it.name }
            ?: error("未找到 build-tools（需要 d8 / zipalign / apksigner）")

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

    private companion object {
        val DEX_NAME_REGEX = Regex("classes\\d*\\.dex")
        val uppercaseSignaturePrefixes = listOf(".SF", ".RSA", ".DSA", ".EC", ".MF")
        const val XOR_BASE = 0x5A
    }
}
