plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    // DetailPane 的 ViewModelStoreOwner 需要 LocalViewModelStoreOwner / ViewModelStore
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit)
}