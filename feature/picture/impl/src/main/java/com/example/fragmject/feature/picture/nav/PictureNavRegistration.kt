package com.example.fragmject.feature.picture.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.example.fragmject.core.navigation.contract.LocalNavigators
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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Picture Feature 导航内容贡献者：注册本域 NavKey → 渲染器映射。
 *
 * 三个页面（Selector/Preview/Editor）共享同一进程级单例的 [PictureFlowState]
 * （Hilt @Singleton，降级自 NavFlowScope 通用流程作用域机制）。
 */
object PictureNavContentContributor : NavContentContributor {
    override fun contribute(registry: NavContentRegistry) {
        registry.register<PictureSelectorNavKey> {
            val pictureFlowState = rememberPictureFlowState()
            val pictureNavigator = LocalNavigators.current.picture
            val onNavigateUp = LocalOnNavigateUp.current
            PictureSelectorScreen(
                onConfirm = {
                    pictureNavigator.onPictureSelected(pictureFlowState.selectedUris.value)
                    onNavigateUp()
                },
                onDismiss = {
                    // 取消选择时上报空结果，避免 AppPictureNavigator 单例残留上次选图结果
                    pictureNavigator.onPictureSelected(emptyList())
                    onNavigateUp()
                },
                onPreview = { uris ->
                    // 进入预览前先按 uris 解析出 MediaItem 快照，预览页只读快照
                    pictureFlowState.startPreview(uris)
                    pictureNavigator.openPicturePreview(uris)
                },
                flowState = pictureFlowState,
            )
        }
        registry.register<PicturePreviewNavKey> { _ ->
            val pictureFlowState = rememberPictureFlowState()
            val pictureNavigator = LocalNavigators.current.picture
            val onNavigateUp = LocalOnNavigateUp.current
            PicturePreviewScreen(
                mode = PreviewMode.SELECT,
                previewPosition = 0,
                onConfirm = { onNavigateUp() },
                onDismiss = { onNavigateUp() },
                onOpenEditor = { uri ->
                    pictureNavigator.openPictureEditor(uri.toString())
                },
                flowState = pictureFlowState,
            )
        }
        registry.register<PictureEditorNavKey> { navKey ->
            val pictureFlowState = rememberPictureFlowState()
            val onNavigateUp = LocalOnNavigateUp.current
            val oldUri = navKey.oldUriString.toUri()
            PictureEditorScreen(
                bitmapUri = oldUri,
                onFinish = { _, newUri ->
                    // 防御：仅当保存成功产生有效新 URI 时才替换/删除原图，
                    // 避免失败路径以空 URI 误删原图。
                    if (newUri.toString().isNotBlank()) {
                        pictureFlowState.updateMediaUri(oldUri, newUri)
                        // 挂进程级 flowScope：页面随即出栈，组合作用域会被取消导致原图残留
                        pictureFlowState.deleteMediaAsync(oldUri)
                    }
                    onNavigateUp()
                },
                onDismiss = { onNavigateUp() },
            )
        }
    }
}

/**
 * 经 Hilt 取回进程级单例的 [PictureFlowState]。
 */
@Composable
private fun rememberPictureFlowState(): PictureFlowState {
    val context = LocalContext.current
    return remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PictureFlowStateEntryPoint::class.java,
        ).pictureFlowState()
    }
}

/** Hilt entry point：向 Composable 暴露 @Singleton 的 [PictureFlowState]。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PictureFlowStateEntryPoint {
    fun pictureFlowState(): PictureFlowState
}
