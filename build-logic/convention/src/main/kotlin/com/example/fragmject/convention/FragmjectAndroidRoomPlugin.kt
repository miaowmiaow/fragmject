package com.example.fragmject.convention

import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class FragmjectAndroidRoomPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room3")
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies.apply {
                add("implementation", libs.findLibrary("androidx-room3-runtime").get())
                add("ksp", libs.findLibrary("androidx-room3-compiler").get())
            }
        }
    }
}