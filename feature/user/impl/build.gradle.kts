plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.feature")
    id("fragmject.android.hilt")
}

dependencies {
    api(project(":feature:user:api"))
    // 多页引用 WebNavKey（article/api）
    implementation(project(":feature:article:api"))
    // UserScreen 绑定 SystemNavKey（home/api）
    implementation(project(":feature:home:api"))

    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
}