plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(project(":core:designsystem"))

    implementation(libs.kotlinx.coroutines)
    implementation(libs.coil.compose)
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.junit)
}