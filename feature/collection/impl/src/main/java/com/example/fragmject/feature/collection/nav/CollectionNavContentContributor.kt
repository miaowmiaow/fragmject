package com.example.fragmject.feature.collection.nav

import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
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
        registry.register<MyCollectNavKey> {
            MyCollectScreen()
        }
        registry.register<MyShareNavKey> {
            MyShareScreen()
        }
        registry.register<ShareArticleNavKey> {
            ShareArticleScreen()
        }
    }
}
