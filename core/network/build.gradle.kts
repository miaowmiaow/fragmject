plugins {
    id("fragmject.android.library")
    id("fragmject.android.feature")
}

android {
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    api(libs.gson)
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutines)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    api(libs.retrofit)
}
