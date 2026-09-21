plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:android-platform"))

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}
