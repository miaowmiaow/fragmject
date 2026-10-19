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
        val lintBaseline = file("lint-baseline.xml")

        configure<LibraryExtension> {
            namespace = defaultNamespace()
            compileSdk = config.compileSdk
            defaultConfig { minSdk = config.minSdk }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            lint.configureFragmjectLint(lintBaseline)
        }
    }
}
