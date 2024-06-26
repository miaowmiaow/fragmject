package com.example.fragment.project.ui.demo

import android.util.Log
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import coil.compose.rememberAsyncImagePainter
import com.example.fragment.project.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * https://gist.github.com/JolandaVerhoef/bcaf96360b92bba974e3796fe37247e2
 */

class Photo(
    val id: Int
) {
    private val avatarList: List<Int> = listOf(
        R.mipmap.avatar_1_raster,
        R.mipmap.avatar_2_raster,
        R.mipmap.avatar_3_raster,
        R.mipmap.avatar_4_raster,
        R.mipmap.avatar_5_raster,
        R.mipmap.avatar_6_raster,
    )

    fun getAvatarRes(): Int {
        var index = 0
        try {
            if (id >= 0) {
                index = id % 6
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return avatarList[index]
    }
}

@Composable
fun GridSelectScreen(
    photos: List<Photo> = List(100) { Photo(it) },
    selectedIds: MutableState<Set<Int>> = rememberSaveable { mutableStateOf(emptySet()) }
) {
    val data = remember {
        mutableListOf<Photo>().also {
            it.addAll(photos)
        }
    }
    val state = rememberLazyGridState()
    val autoScrollSpeed = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(autoScrollSpeed.floatValue) {
        if (autoScrollSpeed.floatValue != 0f) {
            while (isActive) {
                state.scrollBy(autoScrollSpeed.floatValue)
                delay(10)
            }
        }
    }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 80.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.photoGridDragHandler(
            state = state,
            haptics = LocalHapticFeedback.current,
            selectedIds = selectedIds,
            autoScrollSpeed = autoScrollSpeed,
            autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() }
        )
    ) {
        items(data, key = { it.id }) { photo ->
            val selected by remember { derivedStateOf { selectedIds.value.contains(photo.id) } }
            ImageItem(photo, selected, Modifier)
        }
    }
}

fun Modifier.photoGridDragHandler(
    state: LazyGridState,
    haptics: HapticFeedback,
    selectedIds: MutableState<Set<Int>>,
    autoScrollSpeed: MutableState<Float>,
    autoScrollThreshold: Float
) = this.then(pointerInput(Unit) {
    fun LazyGridState.gridItemKeyAtPosition(hitPoint: Offset): Int? =
        layoutInfo.visibleItemsInfo.find { itemInfo ->
            itemInfo.size.toIntRect().contains(hitPoint.round() - itemInfo.offset)
        }?.key as? Int

    var currentKey = -1
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            state.gridItemKeyAtPosition(offset)?.let { key ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                currentKey = key
                if (!selectedIds.value.contains(key)) {
                    selectedIds.value += key
                } else {
                    selectedIds.value -= key
                }
            }
        },
        onDragCancel = { autoScrollSpeed.value = 0f },
        onDragEnd = { autoScrollSpeed.value = 0f },
        onDrag = { change, _ ->
            val distFromBottom = state.layoutInfo.viewportSize.height - change.position.y
            val distFromTop = change.position.y
            autoScrollSpeed.value = when {
                distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                else -> 0f
            }

            state.gridItemKeyAtPosition(change.position)?.let { key ->
                if (currentKey != key) {
                    val range = if (key > currentKey) {
                        (currentKey + 1)..key
                    } else {
                        key until currentKey
                    }
                    range.forEach { i ->
                        if (i !in selectedIds.value) {
                            selectedIds.value += i
                        } else {
                            selectedIds.value -= i
                        }
                    }
                    currentKey = key
                }
            }
        }
    )
})

@Composable
private fun ImageItem(
    photo: Photo,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.aspectRatio(1f),
        tonalElevation = 3.dp
    ) {
        Box {
            val transition = updateTransition(selected, label = "selected")
            val padding by transition.animateDp(label = "padding") { selected ->
                if (selected) 10.dp else 0.dp
            }
            val roundedCornerShape by transition.animateDp(label = "corner") { selected ->
                if (selected) 16.dp else 0.dp
            }
            Image(
                painter = rememberAsyncImagePainter(photo.getAvatarRes()),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .padding(padding)
                    .clip(RoundedCornerShape(roundedCornerShape))
            )
            if (selected) {
                val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                Icon(
                    Icons.Filled.CheckCircle,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .border(2.dp, bgColor, CircleShape)
                        .clip(CircleShape)
                        .background(bgColor)
                )
            } else {
                Icon(
                    Icons.Filled.Face,
                    tint = Color.White.copy(alpha = 0.7f),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}
