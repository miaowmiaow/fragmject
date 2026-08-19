plugins {
    id("fragmject.android.library")
    id("fragmject.android.feature")
}

dependencies {
    implementation(project(":core:network"))
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
