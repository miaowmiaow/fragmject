package com.example.fragmject.feature.auth.nav

import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.auth.RegisterNavKey
import com.example.fragmject.feature.auth.ui.login.LoginScreen
import com.example.fragmject.feature.auth.ui.register.RegisterScreen

/**
 * Auth Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object AuthNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<LoginNavKey> {
            LoginScreen()
        }
        registry.register<RegisterNavKey> {
            RegisterScreen()
        }
    }
}
