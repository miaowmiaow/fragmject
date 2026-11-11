package com.example.fragmject.convention

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
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
import java.util.Properties
import javax.inject.Inject

/**
 * 资源混淆（H4）：对最终 release APK 做 `aapt2 optimize --shorten-resource-paths`。
 *
 * ============================ 为什么放在「构建后处理」 ============================
 * 官方 `AndResGuard` 已归档，社区 fork 只适配到 AGP 8；AGP 9 又把任务访问 API 收掉了大半。
 * 与其 fork 一个插件跟着 AGP 版本走，不如用 AAPT2 自带的能力对**产物**做一次处理：
 * - 不依赖 AGP 内部 API，AGP 10 也不会失效；
 * - 只改 `resources.arsc` 里的路径（`res/drawable-xhdpi/abc.png` → `res/Dg`），
 *   资源 ID 与资源名不变，代码里的 `R.xxx` 不受影响；
 * - 代价是必须重新签名（这一步天然与 H1 的签名校验联动）。
 *
 * ============================ 只对 APK 生效 ============================
 * 国内商店接受 APK；AAB 需先 bundletool 出 APK 才能处理，本项目不发布 AAB。
 *
 * ============================ 开关 ============================
 * `-PenableResObf=true` 时注册；默认关闭，未开启时完全不产生额外任务。
 */
internal fun Project.configureResourceObfuscation() {
    val enabled = providers.gradleProperty(PROP_ENABLE_RES_OBF).map(String::toBoolean).getOrElse(false)
    if (!enabled) return

    val sdkDirectory = extensions
        .getByType(ApplicationAndroidComponentsExtension::class.java)
        .sdkComponents
        .sdkDirectory

    val baseDir = projectDir.absolutePath
    val signingProperties = rootProject.layout.projectDirectory.file("keystore.properties")
    val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
    val obfApk = layout.buildDirectory.file("outputs/apk/release/app-release-resobf.apk")

    val obfTask = tasks.register<ObfuscateResourcesTask>("obfuscateReleaseResources") {
        group = "build"
        description = "缩短 release APK 的资源路径并重签名（H4）"
        inputApk.set(releaseApk)
        buildToolsRoot.set(sdkDirectory.map { it.dir("build-tools") })
        signingPropertiesFile.set(signingProperties)
        signingBaseDir.set(baseDir)
        outputApk.set(obfApk)
    }

    // packageRelease 产出 apk 之后才能处理；assembleRelease 完成后自动带上该任务
    obfTask.configure { dependsOn(tasks.named("packageRelease")) }
    tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(obfTask) }
}

/** 开关名：`-PenableResObf=true`。 */
private const val PROP_ENABLE_RES_OBF = "enableResObf"

/**
 * 对 APK 执行：aapt2 optimize（缩短资源路径）→ zipalign → apksigner 重签。
 *
 * 三步都不可省：
 * - `aapt2 optimize` 输出的包未对齐、且原有签名已失效；
 * - `zipalign -p 4` 保证 4 字节对齐与 so 页对齐（16KB 设备要求）；
 * - 重签必须用**发布证书**，否则 H1 的签名校验会把自己的产物判成改包。
 */
abstract class ObfuscateResourcesTask : DefaultTask() {

    @get:InputFile
    abstract val inputApk: RegularFileProperty

    /** SDK 的 `build-tools` 根目录（取其中最高版本）。 */
    @get:InputDirectory
    abstract val buildToolsRoot: DirectoryProperty

    /** `keystore.properties`：与 release signingConfig 同源，避免出现第二份签名配置。 */
    @get:InputFile
    abstract val signingPropertiesFile: RegularFileProperty

    /** `storeFile` 在 keystore.properties 里是相对 app 模块的路径，需要基准目录解析。 */
    @get:Input
    abstract val signingBaseDir: Property<String>

    @get:OutputFile
    abstract val outputApk: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun run() {
        val tools = resolveBuildTools()
        val work = temporaryDir.apply { deleteRecursively(); mkdirs() }

        val base = File(work, "base.apk")
        inputApk.get().asFile.copyTo(base, overwrite = true)

        val optimized = File(work, "optimized.apk")
        val shorteningMap = File(work, "resource-paths.map")
        run(tools, "aapt2", "optimize", "--shorten-resource-paths",
            "--resource-path-shortening-map", shorteningMap.absolutePath,
            "-o", optimized.absolutePath, base.absolutePath)

        val aligned = File(work, "aligned.apk")
        run(tools, "zipalign", "-p", "-f", "4", optimized.absolutePath, aligned.absolutePath)

        val signed = File(work, "signed.apk")
        val signing = readSigningConfig()
        run(tools, "apksigner", "sign",
            "--ks", signing.storeFile,
            "--ks-pass", "pass:${signing.storePassword}",
            "--ks-key-alias", signing.keyAlias,
            "--key-pass", "pass:${signing.keyPassword}",
            "--out", signed.absolutePath, aligned.absolutePath)

        val output = outputApk.get().asFile
        output.parentFile?.mkdirs()
        signed.copyTo(output, overwrite = true)
        logger.lifecycle("资源混淆完成：${output.absolutePath}（${output.length() / 1024} KB）")
    }

    private fun run(tools: File, tool: String, vararg args: String) {
        val executable = File(tools, tool)
        check(executable.exists()) { "找不到 $tool（${executable.absolutePath}）" }
        exec.exec {
            commandLine(listOf(executable.absolutePath) + args)
            // 不把命令行写进日志：apksigner 的参数里带密码
            standardOutput = java.io.ByteArrayOutputStream()
            errorOutput = java.io.ByteArrayOutputStream()
        }
    }

    private fun resolveBuildTools(): File =
        buildToolsRoot.get().asFile.listFiles()
            ?.filter { it.isDirectory && File(it, "aapt2").exists() }
            ?.maxByOrNull { it.name }
            ?: error("未找到 build-tools（需要 aapt2 / zipalign / apksigner）：${buildToolsRoot.get().asFile}")

    private data class Signing(
        val storeFile: String,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String,
    )

    private fun readSigningConfig(): Signing {
        val file = signingPropertiesFile.get().asFile
        check(file.exists()) { "缺少 ${file.absolutePath}：资源混淆重签需要发布证书" }
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
