package com.example.fragmject.feature.picture.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.picture.PictureEditorNavKey
import com.example.fragmject.feature.picture.PicturePreviewNavKey
import com.example.fragmject.feature.picture.PictureSelectorNavKey
import com.example.fragmject.feature.picture.di.PictureViewModelEntryPoint
import com.example.fragmject.feature.picture.ui.editor.PictureEditorScreen
import com.example.fragmject.feature.picture.ui.selector.PicturePreviewScreen
import com.example.fragmject.feature.picture.ui.selector.PictureSelectorScreen
import com.example.fragmject.feature.picture.ui.selector.PictureViewModel
import com.example.fragmject.feature.picture.ui.selector.PreviewMode
import dagger.hilt.android.EntryPointAccessors

/**
 * Picture Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 *
 * 三个页面（Selector/Preview/Editor）共享同一个 [PictureViewModel]（Hilt 单例），
 * 实例由 picture 模块内部通过 EntryPoint 获取，不感知 app 组合根。
 */
object PictureNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<PictureSelectorNavKey> { _, callbacks ->
            val pictureViewModel = rememberPictureViewModel()
            PictureSelectorScreen(
                onFinish = {
                    pictureViewModel.clearSelection()
                    callbacks.onNavigateUp()
                },
                onDismiss = {
                    pictureViewModel.clearSelection()
                    callbacks.onNavigateUp()
                },
                onPreview = { uris ->
                    callbacks.onNavigate(PicturePreviewNavKey(uris))
                },
                viewModel = pictureViewModel,
            )
        }
        registry.register<PicturePreviewNavKey> { navKey, callbacks ->
            val pictureViewModel = rememberPictureViewModel()
            PicturePreviewScreen(
                mode = PreviewMode.SELECT,
                origSelectUris = navKey.uris,
                previewPosition = 0,
                onFinish = { callbacks.onNavigateUp() },
                onDismiss = { callbacks.onNavigateUp() },
                onOpenEditor = { uri ->
                    callbacks.onNavigate(PictureEditorNavKey(uri.toString()))
                },
                viewModel = pictureViewModel,
            )
        }
        registry.register<PictureEditorNavKey> { navKey, callbacks ->
            val pictureViewModel = rememberPictureViewModel()
            val oldUri = navKey.oldUriString.toUri()
            PictureEditorScreen(
                bitmapUri = oldUri,
                onFinish = { _, newUri ->
                    pictureViewModel.updateMediaUri(oldUri, newUri)
                    pictureViewModel.deleteMedia(oldUri)
                    callbacks.onNavigateUp()
                },
                onDismiss = { callbacks.onNavigateUp() },
            )
        }
    }
}

/**
 * 在 Composable 中获取单例 [PictureViewModel]。
 */
@Composable
private fun rememberPictureViewModel(): PictureViewModel {
    val context = LocalContext.current
    return remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PictureViewModelEntryPoint::class.java
        ).pictureViewModel()
    }
}
