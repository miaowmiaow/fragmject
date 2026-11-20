plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

android {
    testOptions {
        // AuthenticatorAddViewModel 的落盘失败分支会调用 android.util.Log.e；
        // JVM 单测默认抛 "Method not mocked"，异常会打断「复位 saving + 发失败事件」，
        // 表现为按钮永久禁用 / 事件收不到而挂到超时。Log 无副作用，返回默认值即可
        // （与 feature:home:impl、feature:demo:impl 处理保持一致）。
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // 组合根 :app 显式声明 :feature:authenticator:api（见 dependencies-policy.md §6.5）。
    // 此处用 implementation 而非 api()：否则 api 层会经 impl 传递进入 app，
    // 使 APP_ALLOWED_DEPENDENCIES 对契约层空转（守卫只看直接边）。
    implementation(project(":feature:authenticator:api"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))
    // 二维码导出：复用 core:webview 承载本地 H5 页面（不引入二维码生成库）
    implementation(project(":core:webview"))
    // 扫码导入：复用 core:qrscanner（CameraX + ML Kit 由该引擎独占持有）
    implementation(project(":core:qrscanner"))
    // 扫码页自己做权限判断与申请（未授权时整块换成引导卡片，避免引擎静默不绘制）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}