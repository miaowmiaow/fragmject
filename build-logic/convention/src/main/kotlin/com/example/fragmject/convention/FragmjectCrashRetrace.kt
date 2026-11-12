package com.example.fragmject.convention

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * 崩溃栈还原（retrace）任务。
 *
 * ============================ 为什么需要 ============================
 * release 打开 R8 后，业务代码的类名/方法名被混淆、行号被重排，崩溃栈长这样：
 *
 * ```
 * java.lang.IllegalStateException: ...
 *     at a.b.c(SourceFile:42)
 * ```
 *
 * 只有配合同一次构建产出的 `mapping.txt` 才能还原成原始类名与行号。
 * **壳包尤其需要**：业务 dex 被塞进加密载荷，崩溃栈里的符号与常规包一致，
 * 但排查的人往往会误以为「壳导致栈不可读」——其实只是没做 retrace。
 *
 * ============================ 为什么用 build-tools 里的 R8 ============================
 * `build-tools/<ver>/lib/d8.jar` 内含 `com.android.tools.r8.retrace.Retrace`，
 * 与构建期 R8 同源，不需要额外声明 `com.android.tools:r8` 依赖（避免版本漂移）。
 */
abstract class RetraceCrashTask : DefaultTask() {

    /** 与崩溃包同一次构建产出的 mapping。 */
    @get:InputFile
    abstract val mappingFile: RegularFileProperty

    /** 待还原的崩溃栈文本（`adb logcat -d -b crash > stack.txt` 的产物）。 */
    @get:InputFile
    abstract val stackFile: RegularFileProperty

    /** build-tools 根目录，用于定位其中的 `d8.jar`（内含 Retrace）。 */
    @get:InputDirectory
    abstract val buildToolsRoot: DirectoryProperty

    /** 还原结果输出文件。 */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val verbose: Property<Boolean>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun run() {
        val mapping = mappingFile.get().asFile
        if (!mapping.exists()) {
            throw GradleException(
                "缺少 mapping.txt：${mapping.absolutePath}\n" +
                    "请先执行 `./gradlew :app:assembleRelease`（mapping 与崩溃包必须是同一次构建）。",
            )
        }
        val stack = stackFile.get().asFile
        if (!stack.exists()) {
            throw GradleException(
                "缺少崩溃栈文件：${stack.absolutePath}\n" +
                    "用 `adb logcat -d -b crash > ${stack.name}` 抓取后重试。",
            )
        }

        val r8Jar = locateR8Jar()
        val out = outputFile.get().asFile.apply { parentFile.mkdirs() }

        val stdout = java.io.ByteArrayOutputStream()
        val stderr = java.io.ByteArrayOutputStream()
        val result = exec.exec {
            // 用 Gradle 守护进程自己的 JDK：避免依赖 PATH 里的 java 版本
            commandLine(
                File(System.getProperty("java.home"), "bin/java").absolutePath,
                "-cp", r8Jar.absolutePath,
                "com.android.tools.r8.retrace.Retrace",
                mapping.absolutePath,
                stack.absolutePath,
            )
            standardOutput = stdout
            errorOutput = stderr
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            throw GradleException("retrace 执行失败（exit=${result.exitValue}）：\n${stderr.toString().trim()}")
        }
        // Retrace 把结果写到 stdout；这里落盘便于对比与归档
        val retraced = stdout.toString().trim()
        if (retraced.isEmpty()) {
            throw GradleException(
                "retrace 没有输出：请确认崩溃栈里的类名确实来自本次构建的 mapping.txt" +
                    "（mapping 与崩溃包必须是同一次构建）。",
            )
        }
        out.writeText(retraced + "\n")

        val text = out.readText()
        logger.lifecycle("崩溃栈已还原 → ${out.absolutePath}")
        logger.lifecycle("----------------------------------------")
        logger.lifecycle(text.trim())
        logger.lifecycle("----------------------------------------")
        logger.lifecycle(
            "提示：原始行号来自 LineNumberTable，若某帧显示 `SourceFile` 而无行号，" +
                "说明该帧对应的类没有保留调试信息（例如壳自身的 StubApplication，它不参与 R8 混淆，无需还原）。",
        )
    }

    /** 在 build-tools 里找带 Retrace 的 `d8.jar`（取版本号最大的一个）。 */
    private fun locateR8Jar(): File =
        buildToolsRoot.get().asFile.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.mapNotNull { File(it, "lib/d8.jar").takeIf(File::exists) }
            ?.firstOrNull()
            ?: throw GradleException(
                "未在 build-tools 下找到 d8.jar（retrace 需要它）：${buildToolsRoot.get().asFile}",
            )
}

/**
 * 注册 `retraceReleaseCrash`。
 *
 * 用法：
 * ```
 * adb logcat -d -b crash > app/build/outputs/crash/stack.txt
 * ./gradlew :app:retraceReleaseCrash                 # 默认读上面那个文件
 * ./gradlew :app:retraceReleaseCrash -Pstack=/tmp/other.txt
 * ```
 */
internal fun Project.configureCrashRetrace() {
    // 与 DexShell/资源混淆一致：build-tools 从 AGP 的 sdkComponents 取，避免写死本机路径
    val sdkDir = extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        .sdkComponents
        .sdkDirectory

    val mapping = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
    val defaultStack = layout.buildDirectory.file("outputs/crash/stack.txt")
    val stackOverride = providers.gradleProperty("stack")
    val out = layout.buildDirectory.file("outputs/retrace/release-retraced.txt")

    tasks.register<RetraceCrashTask>("retraceReleaseCrash") {
        group = "help"
        description = "用 mapping.txt 还原 release / 壳包的崩溃栈（R8 retrace）"
        mappingFile.set(mapping)
        // 默认读 build/outputs/crash/stack.txt；可用 -Pstack=<path> 覆盖
        stackFile.set(stackOverride.map { layout.projectDirectory.file(it) }.orElse(defaultStack))
        buildToolsRoot.set(sdkDir.map { it.dir("build-tools") })
        outputFile.set(out)
        verbose.set(false)
    }
}
