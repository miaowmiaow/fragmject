package com.example.fragmject.feature.auth

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Auth Feature — 认证域路由表。
 *
 * 登录/注册都是全屏页，**故意不**实现 `DetailPaneNavKey`（大屏不做详情面板分栏）。
 */
@Serializable
object LoginNavKey : NavKey

@Serializable
object RegisterNavKey : NavKey