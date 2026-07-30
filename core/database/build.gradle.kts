plugins {
    id("fragmject.android.library")
    id("fragmject.android.feature")
    id("fragmject.android.room")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
}
