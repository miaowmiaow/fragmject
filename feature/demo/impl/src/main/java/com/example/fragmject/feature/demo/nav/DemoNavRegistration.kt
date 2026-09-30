package com.example.fragmject.feature.demo.nav

import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.feature.demo.DemoNavKey
import com.example.fragmject.feature.demo.ui.demo.DemoScreen

/**
 * Demo Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object DemoNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<DemoNavKey> {
            DemoScreen()
        }
    }
}
