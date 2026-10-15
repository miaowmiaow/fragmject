package com.example.fragmject.feature.authenticator.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.fragmject.core.designsystem.AppColors

/**
 * 验证器页面用到的颜色——**本功能唯一的颜色定义处**（勿再散落到别的文件）。
 *
 * 全部引用 `core:designsystem` 的 [AppColors]，不在 feature 里另造色值：
 * - [appBar] = `AppColors.theme`(#272A36)：顶栏背景。两套明暗方案的
 *   `primaryContainer` 都是它，与全局 `TitleBar` 完全同一块色；
 * - [accent] = `AppColors.orange`(#FF8B80)：倒计时环 / 滑动指示条 / FAB / 选中态。
 *   两套明暗方案的 `secondary` 都是它；
 * - [onAccent] = `AppColors.theme`：强调色上的内容色（深字浅底，保证对比度）；
 * - [secondaryText]：次要文字色。项目暗色方案的 `onSurfaceVariant`(#333333)
 *   在深色卡片上看不清，因此按当前方案的明暗动态取可读值。
 *
 * **坑（勿回退）**：两套方案都未定义 `primary`，任何写 `colorScheme.primary` 的地方
 * 都会落回 M3 默认紫色——强调元素必须显式走 [accent]。
 */
object AuthenticatorColors {
    val appBar: Color = AppColors.theme
    val accent: Color = AppColors.orange
    val onAccent: Color = AppColors.theme

    val secondaryText: Color
        @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFFCCCCCC)
        } else {
            Color(0xFF666666)
        }

    /**
     * 常见服务的品牌色，用于账户头像底色；未知发行方按名称稳定散列取色。
     *
     * 这是「账户标识色」而不是主题色——同一账户在任何主题下都是同一个颜色，
     * 方便用户靠颜色快速认出账户（与系统验证器行为一致）。
     */
    fun brandColor(issuer: String): Color = when (issuer.lowercase()) {
        "google" -> Color(0xFF4285F4)
        "github" -> Color(0xFF333333)
        "microsoft" -> Color(0xFF00A4EF)
        "dropbox" -> Color(0xFF0061FF)
        "slack" -> Color(0xFF4A154B)
        "aws", "amazon" -> Color(0xFFFF9900)
        "apple" -> Color(0xFF000000)
        "tencent", "腾讯" -> Color(0xFF0052D9)
        "alibaba", "阿里云" -> Color(0xFFFF6A00)
        else -> FALLBACK_PALETTE[kotlin.math.abs(issuer.hashCode()) % FALLBACK_PALETTE.size]
    }

    private val FALLBACK_PALETTE = arrayOf(
        Color(0xFFC96442),
        Color(0xFF17A34A),
        Color(0xFF4285F4),
        Color(0xFF8E44AD),
        Color(0xFFE67E22),
        Color(0xFF2C7A7B),
    )
}
