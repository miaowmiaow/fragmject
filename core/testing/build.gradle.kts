plugins {
    id("fragmject.android.library")
}

dependencies {
    // 该模块只被各模块的 test 配置使用（testImplementation），不进生产包。
    // 用 api 暴露 junit / coroutines-test，消费方无需重复声明测试基础库。
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
