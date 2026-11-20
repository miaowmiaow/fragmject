package com.example.fragmject.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class FragmjectAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        val config = FragmjectConfig.load(this)

        configure<LibraryExtension> {
            namespace = defaultNamespace()
            compileSdk = config.compileSdk
            // minor 版本（如 37.1）：Compose 1.13 起的 AAR 元数据要求 compileSdk ≥ 37.1
            config.compileSdkMinor?.let { compileSdkMinor = it }
            defaultConfig { minSdk = config.minSdk }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            lint.configureFragmjectLint()
        }

        configureUnitTestJvmArgs()
        configureUnitTestCoverage()
        // H2 字符串加密：与 application 侧同一套开关与白名单
        configureStringFogForLibrary()
    }
}
