package com.example.fragmject.feature.picture.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
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
 * Picture Feature 内容自注册。
 *
 * 三个页面（Selector/Preview/Editor）共享同一个 [PictureViewModel]（Hilt 单例），
 * 实例由 picture 模块内部通过 EntryPoint 获取，app 只调用无参注册入口，
 * 不感知 picture 内部 UI 包结构与状态管理实现。
 */
fun NavContentRegistry.registerPictureNavContents() {
    register<PictureSelectorNavKey> { _, callbacks ->
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
    register<PicturePreviewNavKey> { navKey, callbacks ->
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
    register<PictureEditorNavKey> { navKey, callbacks ->
        val pictureViewModel = rememberPictureViewModel()
        val oldUri = navKey.oldUriString.toUri()
        PictureEditorScreen(
            bitmapUri = oldUri,
            onFinish = { _, newUri ->
                // 编辑保存会另存为新图：用新 Uri 替换选中项绑定的旧 Uri，
                // 并删除旧图，避免相册出现原图 + 编辑图两张重复
                pictureViewModel.updateMediaUri(oldUri, newUri)
                pictureViewModel.deleteMedia(oldUri)
                callbacks.onNavigateUp()
            },
            onDismiss = { callbacks.onNavigateUp() },
        )
    }
}

/**
 * 在 Composable 中获取单例 [PictureViewModel]。
 *
 * PictureViewModel 为 Hilt @Singleton，三个导航 entry 各自调用都会拿到同一实例；
 * remember 仅用于缓存 EntryPoint 查找开销。
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
