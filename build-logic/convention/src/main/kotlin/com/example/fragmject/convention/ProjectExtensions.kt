package com.example.fragmject.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
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
            val props = Properties().apply {
                load(FileInputStream(project.rootProject.file("config.properties")))
            }
            return FragmjectConfig(
                compileSdk = props.getProperty("compileSdkVersion").toInt(),
                minSdk = props.getProperty("minSdkVersion").toInt(),
                targetSdk = props.getProperty("targetSdkVersion").toInt(),
            )
        }
    }
}

/** 将 project path 转为 namespace */
internal fun Project.defaultNamespace(): String =
    "com.example.fragmject.${path.removePrefix(":").replace(":", ".")}"
