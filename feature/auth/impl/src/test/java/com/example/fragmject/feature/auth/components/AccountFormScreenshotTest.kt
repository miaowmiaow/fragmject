package com.example.fragmject.feature.auth.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.fragmject.core.designsystem.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 登录/注册表单的视觉基线。
 *
 * 表单是「标题 + 副标题 + 输入区 + 主按钮 + 底部链接」的纵向堆叠，字号放大后最容易
 * 出现按钮文字被固定高度挤掉；这张图用来固定「标题与按钮文字完整可见」这个预期。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class AccountFormScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accountForm_at1_0x() = capture(1f, "account-form_1.0x")

    @Test
    fun accountForm_at1_43x() = capture(1.43f, "account-form_1.43x")

    private fun capture(fontScale: Float, name: String) {
        composeRule.setContent {
            AppTheme(fontScale = fontScale) {
                AccountForm(
                    texts = AccountFormTexts(
                        title = "登录",
                        subtitle = "使用玩Android 账号登录",
                        buttonText = "登录",
                        bottomLinkText = "没有账号？去注册",
                        isLoading = false,
                    ),
                    onBack = {},
                    onSubmit = {},
                    onBottomLinkClick = {},
                    // 输入区由调用方注入：这里留空，只验证标题/按钮/底部链接的排版
                    fields = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }
}
