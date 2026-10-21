package com.example.fragmject.core.image.editor.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.values
import com.example.fragmject.core.android.platform.media.getBitmapFromPath
import com.example.fragmject.core.android.platform.media.getBitmapFromUri
import com.example.fragmject.core.image.editor.R
import com.example.fragmject.core.image.editor.components.layer.GraffitiLayer
import com.example.fragmject.core.image.editor.components.layer.MosaicLayer
import com.example.fragmject.core.image.editor.components.layer.OnStickerClickListener
import com.example.fragmject.core.image.editor.components.layer.StickerLayer
import com.example.fragmject.core.image.editor.model.StickerAttrs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Stack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun rememberPictureEditorState(): PictureEditorState {
    val context = LocalContext.current
    return remember { PictureEditorState(context) }
}

@Composable
fun PictureEditorCanvas(
    state: PictureEditorState,
    modifier: Modifier = Modifier,
) {
    var downTime by remember { mutableLongStateOf(0L) }
    var wasPressed by remember { mutableStateOf(false) }
    var prevCentroid by remember { mutableStateOf(Offset.Zero) }
    var prevSpan by remember { mutableFloatStateOf(0f) }
    var prevCount by remember { mutableIntStateOf(0) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                if (state.viewSize == IntSize.Zero && it.width > 0) {
                    state.viewSize = it
                    state.initBitmap()
                } else {
                    state.viewSize = it
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        val first = changes.firstOrNull() ?: continue
                        val currentTime = System.currentTimeMillis()

                        val allPressed = changes.all { it.pressed }
                        val anyPressed = changes.any { it.pressed }

                        val centroid = if (changes.size >= 2) {
                            val positions = changes.map { it.position }
                            Offset(
                                positions.sumOf { it.x.toDouble() }.toFloat() / changes.size,
                                positions.sumOf { it.y.toDouble() }.toFloat() / changes.size
                            )
                        } else {
                            first.position
                        }

                        val span = if (changes.size >= 2) {
                            val p0 = changes[0].position
                            val p1 = changes[1].position
                            val dx = p1.x - p0.x
                            val dy = p1.y - p0.y
                            sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        } else 0f

                        val action = when {
                            !wasPressed && allPressed -> {
                                downTime = currentTime
                                MotionEvent.ACTION_DOWN
                            }
                            wasPressed && !anyPressed -> MotionEvent.ACTION_UP
                            wasPressed && allPressed && changes.size != prevCount -> {
                                if (changes.size > prevCount) MotionEvent.ACTION_POINTER_DOWN
                                else MotionEvent.ACTION_POINTER_UP
                            }
                            wasPressed && allPressed -> MotionEvent.ACTION_MOVE
                            else -> -1
                        }

                        if (action >= 0) {
                            val me = MotionEvent.obtain(
                                downTime, currentTime, action,
                                first.position.x, first.position.y, 0
                            )
                            val handled = state.handleTouchEvent(me)
                            me.recycle()

                            if (!handled && wasPressed && allPressed && changes.size == prevCount) {
                                if (prevCentroid != Offset.Zero && centroid != Offset.Zero) {
                                    val dx = centroid.x - prevCentroid.x
                                    val dy = centroid.y - prevCentroid.y
                                    if (changes.size == 1) {
                                        state.handleScroll(dx, dy)
                                    }
                                }
                                if (span > 0 && prevSpan > 0 && changes.size >= 2) {
                                    val scaleFactor = span / prevSpan
                                    state.handleScale(scaleFactor, centroid.x, centroid.y)
                                }
                            }

                            if (action == MotionEvent.ACTION_DOWN) {
                                state.handleDoubleTap(first.position.x, first.position.y)
                            }
                        }

                        wasPressed = anyPressed
                        prevCentroid = if (anyPressed) centroid else Offset.Zero
                        prevSpan = if (anyPressed && span > 0) span else 0f
                        prevCount = changes.size

                        changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        state.refreshTrigger
        drawIntoCanvas { canvas ->
            state.draw(canvas.nativeCanvas)
        }
    }
}
