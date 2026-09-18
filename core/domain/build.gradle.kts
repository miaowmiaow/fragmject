plugins {
    id("fragmject.android.library")
    id("fragmject.android.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
}