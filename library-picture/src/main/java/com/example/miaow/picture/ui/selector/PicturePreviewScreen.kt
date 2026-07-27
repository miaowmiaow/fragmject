package com.example.miaow.picture.ui.selector

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.miaow.picture.data.MediaBean
import kotlinx.coroutines.launch

enum class PreviewMode { NORM, SELECT }

@Composable
fun PicturePreviewScreen(
    mode: PreviewMode = PreviewMode.NORM,
    origSelectPosition: List<Int> = emptyList(),
    previewPosition: Int = 0,
    onFinish: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
    onOpenEditor: (Uri) -> Unit = {},
    viewModel: PictureViewModel = viewModel(),
) {
    val currAlbumResult by viewModel.currAlbumResult.collectAsStateWithLifecycle()
    val currSelectPosition = remember { mutableStateListOf<Int>() }
    val currSelectPositionSet = remember { mutableStateMapOf<Int, Unit>() }
    val scope = rememberCoroutineScope()
    var showTitleBar by remember { mutableStateOf(true) }
    var showNavBar by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        currSelectPosition.clear()
        currSelectPositionSet.clear()
        currSelectPosition.addAll(origSelectPosition)
        origSelectPosition.forEach { currSelectPositionSet[it] = Unit }
    }

    val data = remember(currAlbumResult, origSelectPosition, mode) {
        if (mode == PreviewMode.SELECT) {
            origSelectPosition.map { currAlbumResult.getOrElse(it) { MediaBean("", Uri.EMPTY) } }
        } else {
            currAlbumResult.toList()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = if (mode == PreviewMode.SELECT) 0 else previewPosition,
        pageCount = { data.size }
    )

    // 每页独立的缩放状态
    @Stable
    class PageZoomState {
        var scale by mutableFloatStateOf(1f)
        var offsetX by mutableFloatStateOf(0f)
        var offsetY by mutableFloatStateOf(0f)

        fun reset() {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    val zoomStates = remember(data.size) {
        List(data.size) { PageZoomState() }
    }

    // 切换页面时还原上一页缩放
    LaunchedEffect(pagerState.currentPage) {
        val prevSnap = pagerState.currentPage
        zoomStates.forEachIndexed { index, state ->
            if (index != prevSnap) state.reset()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 图片滑动区域
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val zoomState = zoomStates[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            do {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }

                                if (activePointers.size >= 2) {
                                    // 多指：计算缩放和平移
                                    val p1 = activePointers[0]
                                    val p2 = activePointers[1]
                                    val prevCentroid =
                                        (p1.previousPosition + p2.previousPosition) / 2f
                                    val currCentroid = (p1.position + p2.position) / 2f
                                    val prevDist =
                                        (p1.previousPosition - p2.previousPosition).getDistance()
                                    val currDist = (p1.position - p2.position).getDistance()
                                    val zoom = if (prevDist > 0f) currDist / prevDist else 1f

                                    val newScale = (zoomState.scale * zoom).coerceIn(0.3f, 5f)
                                    zoomState.scale = newScale
                                    zoomState.offsetX += (currCentroid - prevCentroid).x
                                    zoomState.offsetY += (currCentroid - prevCentroid).y
                                    activePointers.forEach { it.consume() }
                                } else if (activePointers.size == 1 && zoomState.scale != 1f) {
                                    // 单指 + 非原始尺寸：平移
                                    val change = activePointers.first()
                                    val delta = change.position - change.previousPosition
                                    zoomState.offsetX += delta.x
                                    zoomState.offsetY += delta.y
                                    change.consume()
                                }
                                // 单指 + scale=1：不消费，让 HorizontalPager 处理翻页
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showTitleBar = !showTitleBar
                                showNavBar = !showNavBar
                            },
                            onDoubleTap = {
                                if (zoomState.scale != 1f) {
                                    zoomState.reset()
                                } else {
                                    zoomState.scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = data.getOrNull(page)?.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomState.scale
                            scaleY = zoomState.scale
                            translationX = zoomState.offsetX
                            translationY = zoomState.offsetY
                        }
                )
            }
        }

        // 顶部栏
        AnimatedVisibility(
            visible = showTitleBar,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    Text(
                        "${pagerState.currentPage + 1}/${data.size}",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    TextButton(onClick = {
                        onFinish(currSelectPosition.toList())
                        onDismiss()
                    }) {
                        Text("完成", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }

        // 底部栏
        AnimatedVisibility(
            visible = showNavBar,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 缩略图
                    if (mode == PreviewMode.SELECT) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(data, key = { _, item -> item.uri }) { index, media ->
                                val realPosition = origSelectPosition.getOrElse(index) { index }
                                val isSelected = currSelectPositionSet.containsKey(realPosition)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(
                                            2.dp,
                                            if (isSelected) Color(0xFF508CEE) else Color.Transparent
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                ) {
                                    AsyncImage(
                                        model = media.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (!isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 操作栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 编辑按钮
                        TextButton(onClick = {
                            data.getOrNull(pagerState.currentPage)?.uri?.let { onOpenEditor(it) }
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("编辑", color = Color.White, fontSize = 14.sp)
                        }

                        // 选择框
                        TextButton(onClick = {
                            val realPosition = if (mode == PreviewMode.SELECT) {
                                origSelectPosition.getOrElse(pagerState.currentPage) { pagerState.currentPage }
                            } else pagerState.currentPage

                            if (currSelectPositionSet.containsKey(realPosition)) {
                                currSelectPosition.remove(realPosition)
                                currSelectPositionSet.remove(realPosition)
                            } else if (currSelectPosition.size < 9) {
                                currSelectPosition.add(realPosition)
                                currSelectPositionSet[realPosition] = Unit
                            }
                        }) {
                            val realPosition = if (mode == PreviewMode.SELECT) {
                                origSelectPosition.getOrElse(pagerState.currentPage) { pagerState.currentPage }
                            } else pagerState.currentPage
                            val isSelected = currSelectPositionSet.containsKey(realPosition)

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(2.dp, Color.White, CircleShape)
                                    .then(
                                        if (isSelected) Modifier.background(
                                            Color(0xFF508CEE),
                                            CircleShape
                                        )
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isSelected) "取消选择" else "选择",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
