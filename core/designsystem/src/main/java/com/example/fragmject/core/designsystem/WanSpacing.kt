package com.example.fragmject.core.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全局间距与尺寸令牌，与 [WanColors] 同级定义在 core:designsystem 中。
 * 所有 core:ui 共享组件及 feature 模块应引用此处常量，而非内联 dp 字面量，
 * 以保证深色模式 / 可访问性大字体缩放下的一致性。
 */
object WanSpacing {
    /** 卡片 / 容器水平内边距（16dp） */
    val cardHorizontal: Dp = 16.dp

    /** 卡片内部相邻块间距（10dp） */
    val cardItemGap: Dp = 10.dp

    /** 卡片内部微小间距（5dp），如标题与分割线之间 */
    val cardSmallGap: Dp = 5.dp

    /** 卡片圆角（5dp） */
    val cardCornerRadius: Dp = 5.dp

    /** 头像尺寸（30dp） */
    val avatarSize: Dp = 30.dp

    /** 小按钮高度（20dp） */
    val buttonSmallHeight: Dp = 20.dp

    /** 卡片内缩略图宽度（60dp） */
    val thumbnailWidth: Dp = 60.dp

    /** 底部信息行右侧留白（15dp） */
    val cardFooterEndPadding: Dp = 15.dp

    /** 标签按钮圆角（3dp） */
    val tagCornerRadius: Dp = 3.dp
}
