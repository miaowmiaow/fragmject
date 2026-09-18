package com.example.fragmject.feature.demo.nav

import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.demo.DemoNavKey
import com.example.fragmject.feature.demo.ui.demo.DemoScreen

/**
 * Demo Feature 内容自注册。
 */
fun NavContentRegistry.registerDemoNavContents() {
    register<DemoNavKey> { _, callbacks ->
        DemoScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
}
