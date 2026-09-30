plugins {
    id("fragmject.android.library")
    id("fragmject.android.feature")
}

dependencies {
    api(project(":core:navigation-runtime"))
    api(libs.androidx.navigation3.runtime)
}