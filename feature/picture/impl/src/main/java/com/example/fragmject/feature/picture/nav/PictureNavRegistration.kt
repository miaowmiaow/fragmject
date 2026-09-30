package com.example.fragmject.feature.picture.nav

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import com.example.fragmject.core.navigation.contract.LocalPictureNavigator
import com.example.fragmject.core.navigation.runtime.LocalNavFlowScopes
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.feature.picture.PictureEditorNavKey
import com.example.fragmject.feature.picture.PicturePreviewNavKey
import com.example.fragmject.feature.picture.PictureSelectorNavKey
import com.example.fragmject.feature.picture.state.PictureFlowState
import com.example.fragmject.feature.picture.ui.editor.PictureEditorScreen
import com.example.fragmject.feature.picture.ui.selector.PicturePreviewScreen
import com.example.fragmject.feature.picture.ui.selector.PictureSelectorScreen
import com.example.fragmject.feature.picture.ui.selector.PreviewMode

/**
 * Picture Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 *
 * 三个页面（Selector/Preview/Editor）共享同一 [PictureFlowState]。状态由 app 组合根
 * 经通用 [LocalNavFlowScopes] 下发（PictureFlowScopeContributor 创建），本模块不感知
 * app 组合根，app 组合根也不感知 PictureFlowState 等具体类型。
 */
object PictureNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<PictureSelectorNavKey> {
            val pictureFlowState = rememberPictureFlowState()
            if (pictureFlowState != null) {
                val pictureNavigator = LocalPictureNavigator.current
                val onNavigateUp = LocalOnNavigateUp.current
                PictureSelectorScreen(
                    onConfirm = {
                        pictureNavigator.onPictureSelected(pictureFlowState.selectedUris.value)
                        onNavigateUp()
                    },
                    onDismiss = { onNavigateUp() },
                    onPreview = { uris ->
                        pictureNavigator.openPicturePreview(uris)
                    },
                    flowState = pictureFlowState,
                )
            } else {
                PictureFlowScopeUnavailable()
            }
        }
        registry.register<PicturePreviewNavKey> { navKey ->
            val pictureFlowState = rememberPictureFlowState()
            if (pictureFlowState != null) {
                val pictureNavigator = LocalPictureNavigator.current
                val onNavigateUp = LocalOnNavigateUp.current
                PicturePreviewScreen(
                    mode = PreviewMode.SELECT,
                    origSelectUris = navKey.uris,
                    previewPosition = 0,
                    onConfirm = { onNavigateUp() },
                    onDismiss = { onNavigateUp() },
                    onOpenEditor = { uri ->
                        pictureNavigator.openPictureEditor(uri.toString())
                    },
                    flowState = pictureFlowState,
                )
            } else {
                PictureFlowScopeUnavailable()
            }
        }
        registry.register<PictureEditorNavKey> { navKey ->
            val pictureFlowState = rememberPictureFlowState()
            if (pictureFlowState != null) {
                val onNavigateUp = LocalOnNavigateUp.current
                val oldUri = navKey.oldUriString.toUri()
                PictureEditorScreen(
                    bitmapUri = oldUri,
                    onFinish = { _, newUri ->
                        // 防御：仅当保存成功产生有效新 URI 时才替换/删除原图，
                        // 避免失败路径以空 URI 误删原图。
                        if (newUri.toString().isNotBlank()) {
                            pictureFlowState.updateMediaUri(oldUri, newUri)
                            pictureFlowState.deleteMedia(oldUri)
                        }
                        onNavigateUp()
                    },
                    onDismiss = { onNavigateUp() },
                )
            } else {
                PictureFlowScopeUnavailable()
            }
        }
    }
}

/**
 * 从通用 [LocalNavFlowScopes] 中取回本模块的 [PictureFlowState]。
 */
@Composable
private fun rememberPictureFlowState(): PictureFlowState? {
    val scopes = LocalNavFlowScopes.current
    val state = remember(scopes) {
        scopes.values.filterIsInstance<PictureFlowState>().firstOrNull()
    }
    if (state == null) {
        Log.w(TAG, "PictureFlowState 匹配失败：当前流程作用域中不存在 PictureFlowState")
    }
    return state
}

private const val TAG = "PictureNav"

@Composable
private fun PictureFlowScopeUnavailable() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text("图片流程未初始化", color = Color.White)
    }
}
