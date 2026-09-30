plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

dependencies {
    api(project(":feature:article:api"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:player"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:webview"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}