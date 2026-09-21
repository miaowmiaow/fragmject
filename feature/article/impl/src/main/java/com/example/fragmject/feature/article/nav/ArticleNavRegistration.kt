package com.example.fragmject.feature.article.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.article.VideoDownloadNavKey
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.article.ui.download.VideoDownloadScreen
import com.example.fragmject.feature.article.ui.player.VideoPlayerScreen
import com.example.fragmject.feature.article.ui.web.WebScreen

/**
 * Article Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 */
object ArticleNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<WebNavKey> { navKey, callbacks ->
            key(navKey.url) {
                WebScreen(
                    url = navKey.url,
                    onNavigate = callbacks.onNavigate,
                    onNavigateUp = callbacks.onNavigateUp
                )
            }
        }
        registry.register<VideoDownloadNavKey> { _, callbacks ->
            var currentFilePath by remember { mutableStateOf<String?>(null) }
            val filePath = currentFilePath
            if (filePath != null) {
                VideoPlayerScreen(
                    filePath = filePath,
                    onNavigateUp = { currentFilePath = null },
                )
            } else {
                VideoDownloadScreen(
                    onNavigateUp = callbacks.onNavigateUp,
                    onPlayVideo = { currentFilePath = it },
                )
            }
        }
    }
}
