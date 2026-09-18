plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.feature")
    id("fragmject.android.hilt")
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
}
