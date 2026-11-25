package com.example.fragmject.feature.picture.ui.editor

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
 * 图片编辑器顶栏的视觉基线：验证字号放大后「返回 / 保存」仍完整、没有被挤压。
 *
 * 该顶栏没有固定高度（只有状态栏 inset），属于低风险形态；保留这张图是为了在
 * 后续改动时对比，而不是因为它当前有问题。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class EditorTopBarScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorTopBar_at1_0x() = capture(1f, "editor-topbar_1.0x")

    @Test
    fun editorTopBar_at1_43x() = capture(1.43f, "editor-topbar_1.43x")

    private fun capture(fontScale: Float, name: String) {
        composeRule.setContent {
            AppTheme(fontScale = fontScale) {
                EditorTopBar(
                    isSaving = false,
                    onDismiss = {},
                    onSave = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }
}
