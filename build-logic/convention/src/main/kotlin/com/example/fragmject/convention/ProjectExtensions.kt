package com.example.fragmject.convention

import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import java.io.File
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
internal fun Lint.configureFragmjectLint(baseline: File) {
    abortOnError = true
    checkReleaseBuilds = true
    warningsAsErrors = false
    this.baseline = baseline
}