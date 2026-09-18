package com.example.fragmject.feature.auth.nav

import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.auth.RegisterNavKey
import com.example.fragmject.feature.auth.ui.login.LoginScreen
import com.example.fragmject.feature.auth.ui.register.RegisterScreen

/**
 * Auth Feature 内容自注册。
 */
fun NavContentRegistry.registerAuthNavContents() {
    register<LoginNavKey> { _, callbacks ->
        LoginScreen(
            onNavigate = callbacks.onNavigate,
            onNavigateUp = callbacks.onNavigateUp,
            onPopBackStack = callbacks.onPopBackStack,
        )
    }
    register<RegisterNavKey> { _, callbacks ->
        RegisterScreen(
            onNavigateUp = callbacks.onNavigateUp,
            onPopBackStack = callbacks.onPopBackStack,
        )
    }
}
