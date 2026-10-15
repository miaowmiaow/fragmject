plugins {
    id("fragmject.android.library")
    id("fragmject.kotlin.serialization")
}

dependencies {
    // contract 以 api 透传 navigation3-runtime，并承载 DetailPaneNavKey / RequiresAuth 等标记接口；
    // 统一声明，将来给 NavKey 补标记时无需先改 Gradle。
    api(project(":core:navigation-contract"))
    api(libs.androidx.navigation3.runtime)
}