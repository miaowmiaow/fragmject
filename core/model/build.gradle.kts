plugins {
    id("fragmject.android.library")
    id("fragmject.android.feature")
}

dependencies {
    implementation(project(":core:network"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit)
}
