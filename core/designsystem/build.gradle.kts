plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
