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

    implementation(libs.coil.compose)
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.junit)
}