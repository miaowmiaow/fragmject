package com.example.fragmject.core.ui.mapper

import androidx.compose.runtime.Immutable

/**
 * 映射过程中用到的**业务文案与开关**。
 *
 * 这些值原本硬编码在 [Article.toFeedCardUIState] 里，使通用 UI 层带上了文章业务语义
 * （「新」「置顶」「匿名」、封面强制 https）。保留默认值的目的是让现有调用方零改动，
 * 但**任何使用方都可以覆盖**；换业务、换语言、或不要协议升级时不必改 core:ui。
 */
@Immutable
data class ArticleFeedLabels(
    val freshBadge: String = "新",
    val topBadge: String = "置顶",
    val anonymousName: String = "匿名",
    /** 把 `http://` 封面升为 `https://`（图床支持 https，混合协议会被拦截）。 */
    val upgradeCoverToHttps: Boolean = true,
)
