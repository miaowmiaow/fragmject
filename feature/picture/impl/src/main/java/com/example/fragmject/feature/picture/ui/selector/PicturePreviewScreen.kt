package com.example.fragmject.feature.picture.ui.selector

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.state.PictureFlowState
import kotlinx.coroutines.launch

@Composable
fun PicturePreviewScreen(
    mode: PreviewMode = PreviewMode.NORM,
    previewPosition: Int = 0,
    /** 是否展示「编辑」入口。纯看图（网络图 / 只读预览）传 false。 */
    editable: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenEditor: (Uri) -> Unit = {},
    flowState: PictureFlowState,
) {
    val previewSource by flowState.previewSource.collectAsStateWithLifecycle()
    val selectedUriSet by flowState.selectedUriSet.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showTitleBar by remember { mutableStateOf(true) }
    var showNavBar by remember { mutableStateOf(true) }

    // 预览快照：进入预览前由 PictureFlowState.startPreview 一次性解析，
    // 跨相册选择也能正确命中，编辑保存后经 updateMediaUri 同步更新，不随相册切换漂移。
    val data = previewSource

    val pagerState = rememberPagerState(
        initialPage = if (mode == PreviewMode.SELECT) 0 else previewPosition,
        pageCount = { data.size }
    )

    // 每页独立的缩放状态
    val zoomStates = remember(data.size) {
        List(data.size) { PageZoomState() }
    }

    // 切换页面时还原上一页缩放
    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        zoomStates.forEachIndexed { index, state ->
            if (index != currentPage) state.reset()
        }
    }

    val currentMedia = data.getOrNull(pagerState.currentPage)
    val currentUriStr = currentMedia?.uri?.toString()
    val currentSelected = currentUriStr != null && currentUriStr in selectedUriSet

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.editorBackground)
    ) {
        // 图片滑动区域
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            PreviewPage(
                media = data.getOrNull(page),
                zoomState = zoomStates[page],
                onToggleBars = {
                    showTitleBar = !showTitleBar
                    showNavBar = !showNavBar
                },
            )
        }

        // 顶部栏
        AnimatedVisibility(
            visible = showTitleBar,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PreviewTopBar(
                page = pagerState.currentPage,
                total = data.size,
                showConfirm = mode == PreviewMode.SELECT,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
            )
        }

        // 底部栏：纯看图（NORM 且不可编辑）时整条都不渲染，避免留下一条空黑条
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            PreviewBottomSection(
                visible = showNavBar && (editable || mode == PreviewMode.SELECT),
                items = data,
                selectedUriSet = selectedUriSet,
                selection = PreviewBottomSelection(
                    editable = editable,
                    showThumbnails = mode == PreviewMode.SELECT,
                    currentSelected = currentSelected,
                ),
                onSelectThumbnail = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                onEdit = { currentMedia?.uri?.let(onOpenEditor) },
                onToggleSelection = { currentUriStr?.let(flowState::toggleSelection) },
            )
        }
    }
}
