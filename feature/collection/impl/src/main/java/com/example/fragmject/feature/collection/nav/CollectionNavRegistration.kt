package com.example.fragmject.feature.collection.nav

import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.collection.MyCollectNavKey
import com.example.fragmject.feature.collection.MyShareNavKey
import com.example.fragmject.feature.collection.ShareArticleNavKey
import com.example.fragmject.feature.collection.ui.mycollection.MyCollectScreen
import com.example.fragmject.feature.collection.ui.myshare.MyShareScreen
import com.example.fragmject.feature.collection.ui.share.ShareArticleScreen

/**
 * Collection Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object CollectionNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<MyCollectNavKey> { _, callbacks ->
            MyCollectScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
        registry.register<MyShareNavKey> { _, callbacks ->
            MyShareScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
        registry.register<ShareArticleNavKey> { _, callbacks ->
            ShareArticleScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
        }
    }
}
