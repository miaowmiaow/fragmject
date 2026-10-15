package com.example.fragmject.feature.search

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Search Feature — 搜索域路由表。
 *
 * 搜索页是全屏页，**故意不**实现 `DetailPaneNavKey`（大屏不做详情面板分栏）。
 */
@Serializable
data class SearchNavKey(val key: String) : NavKey