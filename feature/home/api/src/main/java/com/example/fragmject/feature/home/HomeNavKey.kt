package com.example.fragmject.feature.home

import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.navigation.DetailPaneNavKey
import kotlinx.serialization.Serializable

/**
 * Home Feature — 首页域路由表。
 *
 * 首页 Tab 容器 + 知识体系文章列表。
 */
@Serializable
object MainNavKey : NavKey

@Serializable
data class SystemNavKey(val cid: String) : DetailPaneNavKey