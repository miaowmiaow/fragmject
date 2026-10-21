package com.example.fragmject.core.image.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize

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
    val gestureHandler = remember(state) { PictureEditorGestureHandler(state) }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                // 首次拿到非零宽度时初始化底图，之后仅同步尺寸
                val firstLayout = state.viewSize == IntSize.Zero && size.width > 0
                state.viewSize = size
                if (firstLayout) state.initBitmap()
            }
            .pointerInput(gestureHandler) {
                awaitPointerEventScope {
                    while (true) {
                        gestureHandler.onEvent(awaitPointerEvent().changes)
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
