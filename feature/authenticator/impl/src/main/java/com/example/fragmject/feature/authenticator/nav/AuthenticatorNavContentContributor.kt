package com.example.fragmject.feature.authenticator.nav

import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import androidx.compose.runtime.key
import com.example.fragmject.feature.authenticator.AuthenticatorAddNavKey
import com.example.fragmject.feature.authenticator.AuthenticatorExportQrNavKey
import com.example.fragmject.feature.authenticator.AuthenticatorNavKey
import com.example.fragmject.feature.authenticator.AuthenticatorTransferNavKey
import com.example.fragmject.feature.authenticator.ui.add.AuthenticatorAddScreen
import com.example.fragmject.feature.authenticator.ui.export.AuthenticatorExportQrScreen
import com.example.fragmject.feature.authenticator.ui.list.AuthenticatorScreen
import com.example.fragmject.feature.authenticator.ui.transfer.AuthenticatorTransferScreen

/**
 * Authenticator 的路由渲染注册。
 *
 * 由 Hilt 以 `Set<NavContentContributor>` 聚合（见 `AuthenticatorNavModule`），
 * app 组合根无需感知本模块即可渲染这三个路由。
 */
object AuthenticatorNavContentContributor : NavContentContributor {

    override fun contribute(registry: NavContentRegistry) {
        registry.register<AuthenticatorNavKey> { AuthenticatorScreen() }
        registry.register<AuthenticatorAddNavKey> { AuthenticatorAddScreen() }
        registry.register<AuthenticatorTransferNavKey> { AuthenticatorTransferScreen() }
        registry.register<AuthenticatorExportQrNavKey> { navKey ->
            // 入参变化时重建页面状态（与 ArticleNavContentContributor 同款处理）
            key(navKey.ids.joinToString(), navKey.batch) {
                AuthenticatorExportQrScreen(ids = navKey.ids, batch = navKey.batch)
            }
        }
    }
}
