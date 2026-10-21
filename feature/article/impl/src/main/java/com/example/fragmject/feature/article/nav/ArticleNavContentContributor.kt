package com.example.fragmject.feature.article.nav

import androidx.compose.runtime.key
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.article.ui.web.WebScreen

/**
 * Article Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object ArticleNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<WebNavKey> { navKey ->
            key(navKey.url) {
                WebScreen(url = navKey.url)
            }
        }
    }
}