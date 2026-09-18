plugins {
    id("fragmject.android.library")
    id("fragmject.android.feature")
    id("fragmject.android.hilt")
}

android {
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.gson)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    testImplementation(libs.junit)
}
