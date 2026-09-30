plugins {
    id("fragmject.android.library")
    id("fragmject.kotlin.parcelize")
    id("fragmject.android.room")
    id("fragmject.android.hilt")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:data-contract"))
    implementation(project(":core:model"))
    implementation(libs.gson)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.room3.paging)
    testImplementation(libs.junit)
}
