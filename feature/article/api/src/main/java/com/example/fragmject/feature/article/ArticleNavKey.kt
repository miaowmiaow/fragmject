package com.example.fragmject.feature.article

import com.example.fragmject.core.common.DetailPaneNavKey
import kotlinx.serialization.Serializable

/**
 * Article Feature — 文章域路由表。
 *
 * WebView 文章详情（跨域共享入口）+ 视频下载。
 */
@Serializable
data class WebNavKey(val url: String) : DetailPaneNavKey

@Serializable
object VideoDownloadNavKey : DetailPaneNavKey