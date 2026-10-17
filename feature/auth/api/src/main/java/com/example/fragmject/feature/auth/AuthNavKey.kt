package com.example.fragmject.feature.auth

import com.example.fragmject.core.navigation.contract.AuthFlowNavKey
import kotlinx.serialization.Serializable

/**
 * Auth Feature — 认证域路由表。
 *
 * 登录/注册都是全屏页，**故意不**实现 `DetailPaneNavKey`（大屏不做详情面板分栏）。
 * 二者均实现 [AuthFlowNavKey]：离开认证流程时组合根据此放弃待回跳目标。
 */
@Serializable
object LoginNavKey : AuthFlowNavKey

@Serializable
object RegisterNavKey : AuthFlowNavKey