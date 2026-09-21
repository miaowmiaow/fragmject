package com.example.fragmject.feature.search.nav

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.search.SearchNavKey
import com.example.fragmject.feature.search.ui.search.SearchScreen

/**
 * Search Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object SearchNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<SearchNavKey> { navKey, callbacks ->
            SearchScreen(key = navKey.key, onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
    }
}
