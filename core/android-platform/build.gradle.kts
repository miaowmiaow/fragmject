plugins {
    id("fragmject.android.library")
    id("fragmject.android.hilt")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)

    // Keystore 真机测试：AndroidKeyStore 无法在 JVM 上模拟（见 KeystoreSecretCipherTest）
    androidTestImplementation(libs.androidx.test.runner)
    // junit:junit 由 androidx.test.ext:junit 传递提供，重复声明只会触发 IDE 重复依赖告警
    androidTestImplementation(libs.androidx.test.ext.junit)
}
