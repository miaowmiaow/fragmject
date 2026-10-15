package com.example.fragmject.feature.authenticator

import com.example.fragmject.core.navigation.contract.DetailPaneNavKey
import kotlinx.serialization.Serializable

/**
 * 二维码展示页路由（导出流程第二步）。
 *
 * 参数显式写在 NavKey 里（与 `WebNavKey(url)` 同款做法）：
 * 展示页只负责渲染，选择状态不依赖任何跨页面的隐式共享状态。
 *
 * @param ids 要导出的账户 id 列表（顺序即展示顺序）
 * @param batch true = 批量 `otpauth-migration://`；false = 每个账户一张 `otpauth://`
 */
@Serializable
data class AuthenticatorExportQrNavKey(
    val ids: List<Long>,
    val batch: Boolean,
) : DetailPaneNavKey
