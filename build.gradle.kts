buildscript {
    dependencies {
//        classpath(libs.miaow.plugin)
    }
}

// Top-level build file. Convention plugins (build-logic) handle most subproject configuration.
// These apply-false declarations are required for the version catalog to resolve plugin versions.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
}