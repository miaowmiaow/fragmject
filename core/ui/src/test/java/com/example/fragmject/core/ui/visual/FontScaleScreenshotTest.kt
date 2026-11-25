package com.example.fragmject.core.ui.visual

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.R
import com.example.fragmject.core.ui.components.ArrowRightItem
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.FeedCardUIState
import com.example.fragmject.core.ui.components.FooterBadge
import com.example.fragmject.core.ui.components.TitleBar
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 字号倍率下的布局基线（JVM 渲染，不需要模拟器）。
 *
 * 为什么需要：字号放大带来的截断（文字被固定高度的容器切掉、圆形裁剪切掉副标题）
 * 只有渲染出来才看得见，静态扫描只能覆盖显式 `height(dp)`，漏掉 `aspectRatio`、
 * `clip`、`Spacer` 组合出来的隐式固定高度。
 *
 * 当前只做「出图 + 入库」：图放在 `core/ui/build/screenshots/`，review 时人工比对。
 * 还没接 diff（Roborazzi 的 compare 模式需要基线目录与 CI 开关），下一步再做。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class FontScaleScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun arrowRightItem_at1_0x() = capture(1f, "arrow-right_1.0x")

    @Test
    fun arrowRightItem_at1_28x() = capture(1.28f, "arrow-right_1.28x")

    /** 1.43x 对应「正文字号 20sp」：用来判断上限能否从 18sp 放开到 20sp。 */
    @Test
    fun arrowRightItem_at1_43x() = capture(1.43f, "arrow-right_1.43x")

    @Test
    fun titleBar_at1_0x() = captureTitleBar(1f, "titlebar_1.0x")

    @Test
    fun titleBar_at1_28x() = captureTitleBar(1.28f, "titlebar_1.28x")

    @Test
    fun titleBar_at1_43x() = captureTitleBar(1.43f, "titlebar_1.43x")

    @Test
    fun feedCard_at1_0x() = captureFeedCard(1f, "feed-card_1.0x")

    @Test
    fun feedCard_at1_43x() = captureFeedCard(1.43f, "feed-card_1.43x")

    /** 卡片是信息密度最高的一类：标题两行 + 描述两行 + 底部角标，最容易堆叠溢出。 */
    private fun captureFeedCard(fontScale: Float, name: String) {
        composeRule.setContent {
            AppTheme(fontScale = fontScale) {
                FeedCard(
                    data = FeedCardUIState(
                        id = "1",
                        title = "Kotlin 2.4 的新特性与 Compose 编译器插件的变化",
                        desc = "这篇梳理了 Kotlin 2.4 在 K2 编译器、增量编译与 Compose 插件上的变化，" +
                            "以及升级时需要注意的兼容性问题。",
                        date = "2026-11-25",
                        userId = "1",
                        link = "https://wanandroid.com/a/1",
                        footerId = "1",
                        footerText = "鸿洋",
                        coverUrl = "",
                        footerBadges = listOf(FooterBadge("置顶"), FooterBadge("新")),
                        selected = false,
                        displayName = "鸿洋",
                        // 头像必须有有效资源 id：传 0 会让 painterResource 直接抛异常
                        avatarResId = R.mipmap.ic_right,
                    ),
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    /**
     * 主题色回归：Slider / Switch 读的是 colorScheme.primary。
     * 曾因 AppDarkColorScheme 未定义 primary 而落到 Material3 默认紫色。
     */
    @Suppress("DEPRECATION")
    @Test
    fun themeControls_at1_0x() = captureControls(1f, "theme-controls_1.0x")

    @Suppress("DEPRECATION")
    @Test
    fun themeControlsDark_at1_0x() = captureControls(1f, "theme-controls-dark_1.0x", darkTheme = true)

    @Suppress("DEPRECATION")
    private fun captureControls(fontScale: Float, name: String, darkTheme: Boolean = false) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme, fontScale = fontScale) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var value by remember { mutableFloatStateOf(15f) }
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 13f..20f,
                        steps = 6,
                    )
                    Row {
                        Switch(checked = true, onCheckedChange = {})
                        Spacer(Modifier.width(16.dp))
                        Switch(checked = false, onCheckedChange = {})
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    /** 顶栏是最容易出现「标题被省略号截断」的位置，特意用长标题 + 右侧动作。 */
    private fun captureTitleBar(fontScale: Float, name: String) {
        composeRule.setContent {
            AppTheme(fontScale = fontScale) {
                TitleBar(
                    title = "一篇很长很长的文章标题，用来验证字号放大后被省略而不是被切掉",
                    onNavigateUp = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun capture(fontScale: Float, name: String) {
        composeRule.setContent {
            AppTheme(fontScale = fontScale) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ArrowRightItem(title = "漂浮宠物", description = "调整大小与速度")
                    ArrowRightItem(title = "字体大小", description = "13~20sp，1sp 步进")
                    ArrowRightItem(title = "关于玩Android", description = "https://wanandroid.com")
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }
}
