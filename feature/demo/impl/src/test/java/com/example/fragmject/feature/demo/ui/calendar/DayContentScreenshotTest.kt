package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 日历日格的视觉基线。
 *
 * 由来（2026-11-25 真机）：18sp 下农历/节日小字被圆形裁剪切成半截 —— 单元格是
 * `aspectRatio(1f)`（高度锁成列宽）+ `clip(CircleShape)`，文字却是 sp，放大后必然溢出。
 * 现在的处理是让日历只跟随系统字体（见 `DayContent` 内的 LocalAppFontScale 说明），
 * 这个测试把这个行为固定下来：1.28x 与 1.0x 的截图应当一致或接近。
 *
 * 单元格尺寸 51dp 取自 1080px / 3x / 7 列的常见值，与 `MonthPager` 的列宽一致。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class DayContentScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dayContent_at1_0x() = capture(1f, "calendar-day_1.0x")

    @Test
    fun dayContent_at1_28x() = capture(1.28f, "calendar-day_1.28x")

    private fun capture(fontScale: Float, name: String) {
        composeRule.setContent {
            AppTheme(fontScale = fontScale) {
                // 元旦：dayLabel 取到节日名，正是真机上被切成半截的那个场景
                Box(modifier = Modifier.size(51.dp)) {
                    DayContent(
                        date = CalendarDate(year = 2026, month = 1, day = 1, week = 4, currMonth = true),
                        isMonthFillMode = false,
                        schedule = listOf("09:00 会议"),
                        isSelected = false,
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }
}
