plugins {
    id("fragmject.android.library")
    id("fragmject.android.feature")
    id("fragmject.android.hilt")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:android-platform"))
    implementation(project(":core:data-contract"))
    implementation(project(":core:model"))
    implementation(libs.androidx.paging.common)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
}
