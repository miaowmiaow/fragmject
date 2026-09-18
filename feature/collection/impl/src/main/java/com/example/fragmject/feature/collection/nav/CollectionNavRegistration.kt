package com.example.fragmject.feature.collection.nav

import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.collection.MyCollectNavKey
import com.example.fragmject.feature.collection.MyShareNavKey
import com.example.fragmject.feature.collection.ShareArticleNavKey
import com.example.fragmject.feature.collection.ui.mycollection.MyCollectScreen
import com.example.fragmject.feature.collection.ui.myshare.MyShareScreen
import com.example.fragmject.feature.collection.ui.share.ShareArticleScreen

/**
 * Collection Feature 内容自注册。
 */
fun NavContentRegistry.registerCollectionNavContents() {
    register<MyCollectNavKey> { _, callbacks ->
        MyCollectScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
    register<MyShareNavKey> { _, callbacks ->
        MyShareScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
    register<ShareArticleNavKey> { _, callbacks ->
        ShareArticleScreen(onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
}
