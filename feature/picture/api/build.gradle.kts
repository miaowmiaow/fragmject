plugins {
    id("fragmject.android.library")
    id("fragmject.kotlin.serialization")
}

dependencies {
    // FlowScopedNavKey 标记接口在 contract，NavKey 实现它需可见
    api(project(":core:navigation-contract"))
    api(libs.androidx.navigation3.runtime)
}
