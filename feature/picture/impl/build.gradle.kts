plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.kotlin.parcelize")
    id("fragmject.android.hilt")
}

dependencies {
    // Picture API (NavKeys)
    api(project(":feature:picture:api"))

    // Core
    implementation(project(":core:android-platform"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:navigation-runtime"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}
