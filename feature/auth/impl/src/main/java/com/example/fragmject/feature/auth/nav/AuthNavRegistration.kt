package com.example.fragmject.feature.auth.nav

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.auth.RegisterNavKey
import com.example.fragmject.feature.auth.ui.login.LoginScreen
import com.example.fragmject.feature.auth.ui.register.RegisterScreen

/**
 * Auth Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object AuthNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<LoginNavKey> { _, callbacks ->
            LoginScreen(
                onNavigate = callbacks.onNavigate,
                onNavigateUp = callbacks.onNavigateUp,
            )
        }
        registry.register<RegisterNavKey> { _, callbacks ->
            RegisterScreen(
                onNavigateUp = callbacks.onNavigateUp,
            )
        }
    }
}
