package com.example.fragmject.feature.authenticator

import com.example.fragmject.core.navigation.contract.DetailPaneNavKey
import kotlinx.serialization.Serializable

/**
 * Authenticator Feature — TOTP 验证码器路由表。
 *
 * - [AuthenticatorNavKey]：账户列表主入口
 * - [AuthenticatorAddNavKey]：新增账户（扫码 / 手输密钥）
 * - [AuthenticatorTransferNavKey]：账号转移（导出二维码）
 */
@Serializable
object AuthenticatorNavKey : DetailPaneNavKey

@Serializable
object AuthenticatorAddNavKey : DetailPaneNavKey

@Serializable
object AuthenticatorTransferNavKey : DetailPaneNavKey
