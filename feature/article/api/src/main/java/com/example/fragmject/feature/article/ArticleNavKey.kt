package com.example.fragmject.feature.article

import com.example.fragmject.core.navigation.runtime.DetailPaneNavKey
import kotlinx.serialization.Serializable

/**
 * Article Feature — 文章域路由表。
 *
 * WebView 文章详情（跨域共享入口）。
 */
@Serializable
data class WebNavKey(val url: String) : DetailPaneNavKey