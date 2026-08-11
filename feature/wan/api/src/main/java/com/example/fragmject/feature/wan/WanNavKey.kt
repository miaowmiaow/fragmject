package com.example.fragmject.feature.wan

import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.common.RequiresAuth
import kotlinx.serialization.Serializable

/**
 * Wan Feature — 统一路由表。
 *
 * ## Home 域
 * - [MainNavKey]           首页 Tab 容器
 * - [SystemNavKey]         知识体系文章列表
 * - [WebNavKey]            WebView 文章详情（跨域共享入口）
 * - [DemoNavKey]           组件 Demo 页
 *
 * ## Search 域
 * - [SearchNavKey]         搜索结果页
 *
 * ## User 域
 * - [LoginNavKey]          登录页
 * - [RegisterNavKey]       注册页
 * - [UserNavKey]           用户主页
 * - [SettingNavKey]        设置页
 * - [MyCoinNavKey]         我的积分 (需登录)
 * - [MyCollectNavKey]      我的收藏 (需登录)
 * - [MyShareNavKey]        我的分享 (需登录)
 * - [ShareArticleNavKey]   新建分享 (需登录)
 * - [BrowseHistoryNavKey]  浏览历史 (需登录)
 * - [RankNavKey]           积分排行榜
 */

// ── Home ──

@Serializable
object MainNavKey : NavKey

@Serializable
data class SystemNavKey(val cid: String) : NavKey

@Serializable
data class WebNavKey(val url: String) : NavKey

@Serializable
object DemoNavKey : NavKey

// ── Search ──

@Serializable
data class SearchNavKey(val key: String) : NavKey

// ── User ──

@Serializable
object LoginNavKey : NavKey

@Serializable
object RegisterNavKey : NavKey

@Serializable
data class UserNavKey(val userId: String) : NavKey

@Serializable
object SettingNavKey : NavKey

@Serializable
object MyCoinNavKey : RequiresAuth

@Serializable
object MyCollectNavKey : RequiresAuth

@Serializable
object MyShareNavKey : RequiresAuth

@Serializable
object ShareArticleNavKey : NavKey

@Serializable
object BrowseHistoryNavKey : NavKey

@Serializable
object RankNavKey : NavKey

@Serializable
object VideoDownloadNavKey : NavKey
