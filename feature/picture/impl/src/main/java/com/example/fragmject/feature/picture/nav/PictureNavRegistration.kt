package com.example.fragmject.feature.picture.nav

import androidx.core.net.toUri
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.picture.PictureEditorNavKey
import com.example.fragmject.feature.picture.PicturePreviewNavKey
import com.example.fragmject.feature.picture.PictureSelectorNavKey
import com.example.fragmject.feature.picture.ui.editor.PictureEditorScreen
import com.example.fragmject.feature.picture.ui.selector.PicturePreviewScreen
import com.example.fragmject.feature.picture.ui.selector.PictureSelectorScreen
import com.example.fragmject.feature.picture.ui.selector.PictureViewModel
import com.example.fragmject.feature.picture.ui.selector.PreviewMode

/**
 * Picture Feature 内容自注册。
 *
 * 三个页面（Selector/Preview/Editor）共享同一个 [PictureViewModel]，因此注册函数
 * 需透传该实例，保证跨页面选中状态一致。
 */
fun NavContentRegistry.registerPictureNavContents(
    pictureViewModel: PictureViewModel,
) {
    register<PictureSelectorNavKey> { _, callbacks ->
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
