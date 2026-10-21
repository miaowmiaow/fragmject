package com.example.fragmject.feature.article.ui.web

import androidx.compose.runtime.Immutable
import com.example.fragmject.core.model.History

/** 底部操作栏依赖的两个页面态：当前是否已收藏、是否已注入脚本。 */
@Immutable
data class WebActionState(
    val bookmark: History?,
    val injectState: Boolean,
)
