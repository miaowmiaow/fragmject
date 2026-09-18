package com.example.fragmject.feature.user

import com.example.fragmject.core.common.DetailPaneNavKey
import com.example.fragmject.core.common.RequiresAuth
import kotlinx.serialization.Serializable

/**
 * User Feature — 用户域路由表。
 *
 * 用户主页、我的积分、积分排行榜、系统设置、浏览历史。
 */
@Serializable
data class UserNavKey(val userId: String) : DetailPaneNavKey

@Serializable
object SettingNavKey : DetailPaneNavKey

@Serializable
object MyCoinNavKey : RequiresAuth, DetailPaneNavKey

@Serializable
object BrowseHistoryNavKey : DetailPaneNavKey

@Serializable
object RankNavKey : DetailPaneNavKey