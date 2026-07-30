plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.feature")
    id("fragmject.android.hilt")
}

dependencies {
    implementation(project(":core:common"))
    api(project(":core:database"))
    api(project(":core:model"))
    implementation(project(":core:network"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
}
