package com.example.fragmject.feature.search.nav

import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.feature.search.SearchNavKey
import com.example.fragmject.feature.search.ui.SearchScreen

/**
 * Search Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object SearchNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<SearchNavKey> { navKey ->
            SearchScreen(key = navKey.key)
        }
    }
}
