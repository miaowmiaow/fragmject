package com.example.fragmject.feature.collection

import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.navigation.DetailPaneNavKey
import com.example.fragmject.core.navigation.RequiresAuth
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

@Serializable
object ShareArticleNavKey : NavKey