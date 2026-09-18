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
    implementation(project(":core:common"))
    api(project(":core:designsystem"))
    api(project(":core:model"))

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}