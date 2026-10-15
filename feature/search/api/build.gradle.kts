plugins {
    id("fragmject.android.library")
    id("fragmject.kotlin.serialization")
}

dependencies {
    api(project(":core:navigation-contract"))
    api(libs.androidx.navigation3.runtime)
}