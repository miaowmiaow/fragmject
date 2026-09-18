plugins {
    id("fragmject.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit)
}
