package com.example.fragmject.convention

import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.FileInputStream
import java.util.Properties

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * 共享工具：加载 config.properties 中的编译参数。
 */
internal data class FragmjectConfig(
    val compileSdk: Int,
    val minSdk: Int,
    val targetSdk: Int,
) {
    companion object {
        fun load(project: Project): FragmjectConfig {
            val props = project.loadConfigProperties()
            return FragmjectConfig(
                compileSdk = props.getProperty("compileSdkVersion").toInt(),
                minSdk = props.getProperty("minSdkVersion").toInt(),
                targetSdk = props.getProperty("targetSdkVersion").toInt(),
            )
        }
    }
}

/** 将 project path 转为 namespace（`:` 与 `-` 均归一化为 `.`）。 */
internal fun Project.defaultNamespace(): String =
    "com.example.fragmject.${path.removePrefix(":").replace(":", ".").replace("-", ".")}"

/** 加载根目录 config.properties，供各模块 build 脚本复用（消除 app / network 的重复加载）。 */
fun Project.loadConfigProperties(): Properties =
    Properties().apply {
        load(FileInputStream(rootProject.file("config.properties")))
    }

/**
 * 统一的 lint 策略，所有 android 模块共用。
 *
 * - `baseline` 必须显式指定：不指定时 `updateLintBaseline` 没有目标文件，跑完不会生成任何东西。
 *   基线按模块各自一份（路径相对模块目录），生成后需提交入库。
 * - 告警不阻断构建（warningsAsErrors = false），只让真正的 lint error 阻断；
 *   基线里冻结的是存量问题，新增问题仍会照常报错。
 * - 个别已知误报/业务需要项，优先在代码或资源里用 `tools:ignore` / `@Suppress` 局部抑制，
 *   例如 app/src/main/res/xml/network_security_config.xml 的 InsecureBaseConfiguration。
 */
/**
 * 单测覆盖率（JaCoCo）。
 *
 * AGP 9 已移除内置的 `enableUnitTestCoverage` / `createDebugUnitTestCoverageReport`，
 * 因此直接挂 Gradle 自带的 `jacoco` 插件：给所有 [Test] 任务注入 agent，
 * 再注册一个聚合 debug 单测的 `unitTestCoverageReport`。
 *
 * class 目录取 AGP 的中间产物约定（Kotlin 编译输出 + Java 编译输出），
 * 报告落在 `build/reports/jacoco/unitTestCoverageReport/`。
 * 若升级 AGP 后产物路径变化，报告会变空——CI 里以"报告里有类"作为校验点。
 */
internal fun Project.configureUnitTestCoverage() {
    pluginManager.apply("jacoco")

    tasks.withType(Test::class.java).configureEach {
        extensions.configure<JacocoTaskExtension> {
            isEnabled = true
            // Robolectric 在自定义 sandbox classloader 中加载被测类，默认会被 agent 跳过，
            // 导致 Robolectric 测试全部计为 0 覆盖。开启后 jdk 内部类会产生大量噪音，需显式排除。
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    tasks.register<JacocoReport>("unitTestCoverageReport") {
        group = "verification"
        description = "生成 debug 单元测试覆盖率报告（JaCoCo）"
        dependsOn("testDebugUnitTest")

        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        classDirectories.setFrom(
            files(
                // AGP 9 起 Kotlin 编译产物在 built_in_kotlinc 下（不再是 tmp/kotlin-classes）；
                // 旧路径一并保留，兼容后续切换与混合产物。
                layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
                layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
                layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes"),
            ).asFileTree.matching {
                // 覆盖率口径 = 可在 JVM 单测执行的逻辑层。
                // Compose 组件需要 Robolectric/设备才能执行，纳入分母只会让数字永久失真，
                // 因此按后缀排除 UI 文件；Hilt/注解处理器生成的类同样不计入。
                exclude(
                    "**/*Screen*.class",
                    "**/*Content*.class",
                    "**/*Sheet*.class",
                    "**/*Card*.class",
                    "**/*Bar*.class",
                    "**/*Overlay*.class",
                    "**/*Dialog*.class",
                    "**/*Picker*.class",
                    "**/*Canvas*.class",
                    "**/*Button*.class",
                    "**/*Indicator*.class",
                    "**/*Ring*.class",
                    "**/*Fab*.class",
                    "**/*EmptyState*.class",
                    "**/*Colors*.class",
                    "**/*Page*.class",
                    // 屏幕拆分后新增的纯 UI 文件（只含 @Composable），命名不在上面的后缀里，
                    // 若不排除会把 Compose 代码算成"未覆盖的逻辑"，让分母虚高、口径失真。
                    "**/*Grid*.class",
                    "**/*Strip*.class",
                    "**/*Section*.class",
                    "**/*Actions*.class",
                    "**/*States*.class",
                    "**/*Row*.class",
                    "**/*NavContentContributor*.class",
                    "**/*NavModule*.class",
                    "**/MonthPager*.class",
                    "**/CalendarKt.class",
                    "**/CalendarKt$*.class",
                    "**/di/**",
                    "**/nav/**",
                    "**/components/**",
                    "**/*_Factory*.class",
                    "**/*_Impl*.class",
                    "**/*HiltModules*.class",
                    "**/_com_*.class",
                )
            },
        )
        sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
        executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    }
}

/**
 * 统一的 lint 策略，所有 android 模块共用。
 *
 * 刻意**不使用 baseline**：AGP 的 baseline 按模块独立生成与校验，收成一份会互相覆盖，
 * 散在 33 个模块里又变成 33 个需要维护的文件。本工程存量告警极少，方向是"修掉"而不是"冻结"。
 *
 * - 告警不阻断构建（warningsAsErrors = false），只有真正的 lint error 阻断。
 * - 已知误报/业务需要项优先在代码里局部 `@Suppress` / `tools:ignore`（如 network_security_config.xml
 *   的 InsecureBaseConfiguration）；确实无法局部抑制的，才在这里全局关闭并写明理由。
 * - `IconMissingDensityFolder`：本工程图标只提供 `drawable-xxhdpi` 一套，由系统按密度缩放，
 *   属有意的体积取舍；补齐全套密度图只会多出几十张重复资源。
 */
internal fun Lint.configureFragmjectLint() {
    abortOnError = true
    checkReleaseBuilds = true
    warningsAsErrors = false
    disable += "IconMissingDensityFolder"
}