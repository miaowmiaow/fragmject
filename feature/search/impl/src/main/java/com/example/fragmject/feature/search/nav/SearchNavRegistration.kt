package com.example.fragmject.feature.search.nav

import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.search.SearchNavKey
import com.example.fragmject.feature.search.ui.search.SearchScreen

/**
 * Search Feature 内容自注册。
 */
fun NavContentRegistry.registerSearchNavContents() {
    register<SearchNavKey> { navKey, callbacks ->
        SearchScreen(key = navKey.key, onNavigate = callbacks.onNavigate, onNavigateUp = callbacks.onNavigateUp)
    }
}
