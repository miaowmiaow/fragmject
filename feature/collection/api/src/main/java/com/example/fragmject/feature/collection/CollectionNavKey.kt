package com.example.fragmject.feature.collection

import com.example.fragmject.core.navigation.contract.DetailPaneNavKey
import com.example.fragmject.core.navigation.contract.RequiresAuth
import kotlinx.serialization.Serializable

/**
 * Collection Feature — 收藏/分享域路由表。
 *
 * 我的收藏、我的分享、新建分享。
 */
@Serializable
object MyCollectNavKey : RequiresAuth, DetailPaneNavKey

@Serializable
object MyShareNavKey : RequiresAuth, DetailPaneNavKey

/** 新建分享：需要登录，但走全屏页（**故意不**实现 DetailPaneNavKey）。 */
@Serializable
object ShareArticleNavKey : RequiresAuth