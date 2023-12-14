package com.example.fragment.project.ui.demo

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 第一个 item 向下拖动时会出现异常滚动，待解决中
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridSortScreen(
    viewModel: PhotosGridViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val lazyGridState = rememberLazyGridState()
    val layoutInfo by remember { derivedStateOf { lazyGridState.layoutInfo } }
    var pressedKey by remember { mutableIntStateOf(-1) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() }

    fun gridItemAtPosition(hitPoint: Offset): LazyGridItemInfo? =
        layoutInfo.visibleItemsInfo.find { itemInfo ->
            IntRect(
                left = itemInfo.offset.x,
                top = itemInfo.offset.y,
                right = itemInfo.offset.x + itemInfo.size.width,
                bottom = itemInfo.offset.y + itemInfo.size.height
            ).contains(hitPoint.round())
        }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    gridItemAtPosition(offset)?.let { info ->
                        pressedKey = info.index
                    }
                },
                onDragEnd = {
                    offsetX = 0f
                    offsetY = 0f
                    pressedKey = -1
                },
                onDragCancel = {
                    pressedKey = -1
                },
                onDrag = { change, offset ->
                    change.consume()
                    offsetX += offset.x
                    offsetY += offset.y
                    gridItemAtPosition(change.position)?.let { info ->
                        val nextKey = info.index
                        if (pressedKey != -1 && pressedKey != nextKey) {
                            viewModel.move(pressedKey, nextKey)
                            offsetX = change.position.x - info.offset.x - info.size.width * 0.5f
                            offsetY = change.position.y - info.offset.y - info.size.height * 0.5f
                            pressedKey = nextKey
                        }
                        coroutineScope.launch {
                            val distFromBottom =
                                lazyGridState.layoutInfo.viewportSize.height - change.position.y
                            val distFromTop = change.position.y
                            lazyGridState.scrollBy(
                                when {
                                    distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                                    distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                                    else -> 0f
                                }
                            )
                        }
                    }
                }
            )
        },
        state = lazyGridState,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        itemsIndexed(uiState.result, key = { _, photo ->
            photo.id
        }) { index, photo ->
            Image(
                painter = rememberAsyncImagePainter(photo.getAvatarRes()),
                contentDescription = null,
                modifier = Modifier
                    .scale(
                        updateTransition(pressedKey == index, label = "selected")
                            .animateFloat(label = "scale") { selected ->
                                if (selected) 0.9f else 1f
                            }.value
                    )
                    .aspectRatio(1f)
                    .then(
                        if (pressedKey == index) {
                            Modifier
                                .offset {
                                    IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                                }
                                .zIndex(1f)
                                .shadow(8.dp)
                        } else {
                            Modifier
                                .zIndex(0f)
                                .shadow(0.dp)
                                .animateItemPlacement()
                        }
                    )
            )
        }
    }
}

data class PhotosGridUiState(
    var result: MutableList<Photo> = ArrayList(),
    val updateTime: Long = 0
)

class PhotosGridViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(PhotosGridUiState())

    val uiState: StateFlow<PhotosGridUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { state ->
            List(50) {
                state.result.add(Photo(it))
            }
            state.copy(updateTime = System.nanoTime())
        }
    }

    fun move(from: Int, to: Int) {
        _uiState.update { state ->
            state.result.add(to, state.result.removeAt(from))
            state.copy(updateTime = System.nanoTime())
        }
    }
}