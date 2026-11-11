package com.example.fragmject.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class FragmjectAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            val config = FragmjectConfig.load(this)

            configure<ApplicationExtension> {
                compileSdk = config.compileSdk
                defaultConfig {
                    minSdk = config.minSdk
                    targetSdk = config.targetSdk
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
                lint.configureFragmjectLint()
            }

            configureUnitTestCoverage()
            // H2 字符串加密：默认关闭，`-PenableStringFog=true` 时对 release/debug 全部变体插桩
            configureStringFogForApplication()
            // H4 资源混淆：默认关闭，`-PenableResObf=true` 时在 packageRelease 之后处理产物
            configureResourceObfuscation()
        }
    }
}
