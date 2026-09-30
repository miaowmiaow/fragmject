plugins {
    id("fragmject.android.library")
}

android {
    namespace = "com.example.fragmject.core.navigation.contract"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.kotlin.stdlib)
    // 语义 Navigator 的 Compose 访问入口（CompositionLocal）需要 compose-runtime
    implementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit)
}
