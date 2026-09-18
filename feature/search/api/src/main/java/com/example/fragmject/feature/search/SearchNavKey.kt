package com.example.fragmject.feature.search

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Search Feature — 搜索域路由表。
 */
@Serializable
data class SearchNavKey(val key: String) : NavKey