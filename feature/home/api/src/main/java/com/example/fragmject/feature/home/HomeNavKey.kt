package com.example.fragmject.feature.home

import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.navigation.contract.DetailPaneNavKey
import kotlinx.serialization.Serializable

/**
 * Home Feature — 首页域路由表。
 *
 * 首页 Tab 容器 + 知识体系文章列表。
 *
 * 未实现 `DetailPaneNavKey` 的即全屏页（大屏不做详情面板分栏），
 * 实现该标记的走 `isDetailPaneKey` 分栏渲染。
 */

/** 首页 Tab 容器根路由：全屏页，**故意不**实现 DetailPaneNavKey。 */
@Serializable
object MainNavKey : NavKey

@Serializable
data class SystemNavKey(val cid: String) : DetailPaneNavKey