plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit)
}