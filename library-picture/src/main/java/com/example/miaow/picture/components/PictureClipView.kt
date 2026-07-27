package com.example.miaow.picture.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.values
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Compose 版本的图片裁剪视图
 */
@Composable
fun rememberPictureClipState(): PictureClipState {
    return remember { PictureClipState() }
}

@Stable
class PictureClipState {
    var orgBitmap: Bitmap? by mutableStateOf(null)
    private var _bitmap by mutableStateOf<Bitmap?>(null)
    var viewSize by mutableStateOf(IntSize.Zero)
    var refreshTrigger by mutableLongStateOf(0L)

    private val bitmapMatrix = Matrix()
    private val bitmapRectF = RectF()
    val clipRectF = RectF()
    private val maxClipRectF = RectF()

    private var paddingTop = 0f
    private var paddingBottom = 0f

    private var isLeftDrag = false
    private var isTopDrag = false
    private var isRightDrag = false
    private var isBottomDrag = false
    private var isBitmapDrag = false
    private var isScaling = false
    private var downX = 0f
    private var downY = 0f

    fun setBitmap(bitmap: Bitmap) {
        this.orgBitmap = bitmap
        this._bitmap = bitmap
        computeBitmapRectF()
        computeDragRectF()
        clipBorderCenter()
        refreshTrigger++
    }

    fun setPadding(top: Float, bottom: Float) {
        paddingTop = top
        paddingBottom = bottom
        if (orgBitmap != null) {
            computeBitmapRectF()
            computeDragRectF()
            clipBorderCenter()
        }
    }

    fun reset() {
        orgBitmap?.let { bmp ->
            _bitmap = bmp
            bitmapMatrix.reset()
            bitmapRectF.set(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
            computeBitmapRectF()
            computeDragRectF()
            clipBorderCenter()
            refreshTrigger++
        }
    }

    fun rotate() {
        val bmp = _bitmap ?: return
        bitmapMatrix.setRotate(-90f, clipRectF.centerX(), clipRectF.centerY())
        bitmapMatrix.mapRect(bitmapRectF)
        bitmapMatrix.mapRect(clipRectF)
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, bitmapMatrix, true)
        bitmapMatrix.reset()
        _bitmap = rotated
        bitmapRectF.set(0f, 0f, rotated.width.toFloat(), rotated.height.toFloat())
        computeBitmapRectF()
        computeDragRectF()
        clipBorderCenter()
        refreshTrigger++
    }

    fun handleTouchDown(x: Float, y: Float) {
        downX = x
        downY = y
        isLeftDrag = leftDragRectF.contains(x, y)
        isTopDrag = topDragRectF.contains(x, y)
        isRightDrag = rightDragRectF.contains(x, y)
        isBottomDrag = bottomDragRectF.contains(x, y)
    }

    fun handleTouchMove(x: Float, y: Float) {
        if (isScaling) return
        var changed = false
        if (!isBitmapDrag) {
            if (isLeftDrag) { clipRectF.left = min(x, rightDragRectF.left); changed = true }
            if (isTopDrag) { clipRectF.top = min(y, bottomDragRectF.top); changed = true }
            if (isRightDrag) { clipRectF.right = max(x, leftDragRectF.right); changed = true }
            if (isBottomDrag) { clipRectF.bottom = max(y, topDragRectF.bottom); changed = true }
            // 确保裁剪框内的图片不超出图片范围
            if (bitmapRectF.width() < clipRectF.width()) {
                val scaleFactor = clipRectF.width() / bitmapRectF.width()
                val px = bitmapRectF.centerX()
                val py = (bitmapRectF.top - DRAG_WIDTH) + clipRectF.centerY()
                bitmapMatrix.setScale(scaleFactor, scaleFactor, px, py)
                bitmapMatrix.mapRect(bitmapRectF)
                bitmapRectF.offset(clipRectF.left - bitmapRectF.left, 0f)
            }
            if (bitmapRectF.height() < clipRectF.height()) {
                val scaleFactor = clipRectF.height() / bitmapRectF.height()
                val px = (bitmapRectF.left - DRAG_WIDTH) + clipRectF.centerX()
                val py = bitmapRectF.centerY()
                bitmapMatrix.setScale(scaleFactor, scaleFactor, px, py)
                bitmapMatrix.mapRect(bitmapRectF)
                bitmapRectF.offset(0f, clipRectF.top - bitmapRectF.top)
            }
        }
        if (!isDragging && bitmapRectF.contains(x, y)) {
            isBitmapDrag = true
            bitmapRectF.offset(x - downX, y - downY)
            downX = x
            downY = y
            changed = true
        }
        if (changed) refreshTrigger++
    }

    fun handleTouchUp() {
        resetBitmapRectF()
        clipBorderCenter()
        resetState()
    }

    fun handleScale(scaleFactor: Float, focusX: Float, focusY: Float) {
        val currScale = bitmapMatrix.values()[0]
        var factor = scaleFactor
        if (currScale * factor <= MINIMUM_SCALE) factor = MINIMUM_SCALE / currScale
        if (currScale * factor >= MAXIMUM_SCALE) factor = MAXIMUM_SCALE / currScale
        if (abs(factor - 1f) > 0.01f) {
            isScaling = true
            bitmapMatrix.setScale(factor, factor, focusX, focusY)
            bitmapMatrix.mapRect(bitmapRectF)
            refreshTrigger++
        }
    }

    fun finishScaling() {
        isScaling = false
    }

    fun saveBitmap(): Bitmap {
        val bmp = _bitmap ?: return createBitmap(1, 1)
        val width = max(clipRectF.width().toInt(), 1)
        val height = max(clipRectF.height().toInt(), 1)
        val clipBitmap = createBitmap(width, height)
        val canvas = Canvas(clipBitmap)
        val left = -clipRectF.left
        val top = -clipRectF.top
        val tempClip = RectF(clipRectF)
        tempClip.offset(left, top)
        val tempBitmapRect = RectF(bitmapRectF)
        tempBitmapRect.offset(left, top)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawRect(tempClip, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.drawBitmap(bmp, null, tempBitmapRect, paint)
        return clipBitmap
    }

    private fun computeBitmapRectF() {
        if (viewSize == IntSize.Zero) return
        val bmp = _bitmap ?: return
        val maxClipLeft = DRAG_WIDTH
        val maxClipTop = DRAG_WIDTH + paddingTop
        val maxClipRight = viewSize.width - DRAG_WIDTH
        val maxClipBottom = viewSize.height - DRAG_WIDTH - paddingBottom
        maxClipRectF.set(maxClipLeft, maxClipTop, maxClipRight, maxClipBottom)
        val scaleFactor = if (bmp.width > bmp.height) {
            maxClipRectF.width() / bmp.width
        } else {
            maxClipRectF.height() / bmp.height
        }
        val bitmapLeft = (viewSize.width - bmp.width * scaleFactor) * 0.5f
        val bitmapTop = (viewSize.height - bmp.height * scaleFactor) * 0.5f
        val bitmapRight = viewSize.width - bitmapLeft
        val bitmapBottom = viewSize.height - bitmapTop
        bitmapRectF.set(bitmapLeft, bitmapTop, bitmapRight, bitmapBottom)
        clipRectF.set(
            bitmapRectF.left.coerceAtLeast(maxClipRectF.left),
            bitmapRectF.top.coerceAtLeast(maxClipRectF.top),
            bitmapRectF.right.coerceAtMost(maxClipRectF.right),
            bitmapRectF.bottom.coerceAtMost(maxClipRectF.bottom)
        )
    }

    private fun resetBitmapRectF() {
        if (bitmapRectF.width() < clipRectF.width() || bitmapRectF.height() < clipRectF.height()) {
            val a = clipRectF.width() / bitmapRectF.width()
            val b = clipRectF.height() / bitmapRectF.height()
            val scale = max(a, b)
            bitmapMatrix.setScale(scale, scale, bitmapRectF.centerX(), bitmapRectF.centerY())
            bitmapMatrix.mapRect(bitmapRectF)
        }
        if (bitmapRectF.left > clipRectF.left) bitmapRectF.offset(
            clipRectF.left - bitmapRectF.left,
            0f
        )
        if (bitmapRectF.top > clipRectF.top) bitmapRectF.offset(0f, clipRectF.top - bitmapRectF.top)
        if (bitmapRectF.right < clipRectF.right) bitmapRectF.offset(
            clipRectF.right - bitmapRectF.right,
            0f
        )
        if (bitmapRectF.bottom < clipRectF.bottom) bitmapRectF.offset(
            0f,
            clipRectF.bottom - bitmapRectF.bottom
        )
    }

    private val leftDragRectF = RectF()
    private val topDragRectF = RectF()
    private val rightDragRectF = RectF()
    private val bottomDragRectF = RectF()

    private fun computeDragRectF() {
        val left = clipRectF.left - DRAG_WIDTH
        val top = clipRectF.top - DRAG_WIDTH
        val right = clipRectF.right + DRAG_WIDTH
        val bottom = clipRectF.bottom + DRAG_WIDTH
        leftDragRectF.set(left, top, clipRectF.left + DRAG_WIDTH, bottom)
        topDragRectF.set(left, top, right, clipRectF.top + DRAG_WIDTH)
        rightDragRectF.set(clipRectF.right - DRAG_WIDTH, top, right, bottom)
        bottomDragRectF.set(left, clipRectF.bottom - DRAG_WIDTH, right, bottom)
    }

    private fun clipBorderCenter() {
        if (viewSize == IntSize.Zero) return
        val offsetX = (viewSize.width - clipRectF.width()) * 0.5f - clipRectF.left
        val offsetY = (viewSize.height - clipRectF.height()) * 0.5f - clipRectF.top
        clipRectF.offset(offsetX, offsetY)
        bitmapRectF.offset(offsetX, offsetY)
        val a = maxClipRectF.width() / clipRectF.width()
        val b = maxClipRectF.height() / clipRectF.height()
        val scale = min(a, b)
        bitmapMatrix.setScale(scale, scale, clipRectF.centerX(), clipRectF.centerY())
        bitmapMatrix.mapRect(bitmapRectF)
        bitmapMatrix.mapRect(clipRectF)
        computeDragRectF()
    }

    private fun resetState() {
        isLeftDrag = false
        isTopDrag = false
        isRightDrag = false
        isBottomDrag = false
        isBitmapDrag = false
        isScaling = false
    }

    private val isDragging get() = isLeftDrag || isTopDrag || isRightDrag || isBottomDrag

    companion object {
        private const val LINE_NUMBER = 2
        private const val LINE_WIDTH = 2f
        private const val BORDER_WIDTH = 5f
        private const val CORNER_WIDTH = 10f
        private const val CORNER_LENGTH = 50f
        private const val DRAG_WIDTH = 75f
        private const val MINIMUM_SCALE = 0.1f
        private const val MAXIMUM_SCALE = 2.0f
    }

    fun draw(clipCanvas: androidx.compose.ui.graphics.Canvas) {
        val canvas = clipCanvas.nativeCanvas
        val bmp = _bitmap ?: return
        val w = viewSize.width.toFloat()
        val h = viewSize.height.toFloat()
        val l = clipRectF.left
        val t = clipRectF.top
        val r = clipRectF.right
        val b = clipRectF.bottom

        // 绘制图片
        canvas.drawBitmap(bmp, null, bitmapRectF, null)

        // 绘制模糊区域
        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#60000000".toColorInt()
        }
        canvas.drawRect(0f, 0f, w, t, darkPaint)
        canvas.drawRect(0f, t, l, b, darkPaint)
        canvas.drawRect(r, t, w, b, darkPaint)
        canvas.drawRect(0f, b, w, h, darkPaint)

        // 绘制格线
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val columnSpacing = clipRectF.width() / 3
        val rowSpacing = clipRectF.height() / 3
        for (i in 1..LINE_NUMBER) {
            val lineLeft = l + columnSpacing * i
            canvas.drawRect(lineLeft, t, lineLeft + LINE_WIDTH, b, whitePaint)
            val lineTop = t + rowSpacing * i
            canvas.drawRect(l, lineTop, r, lineTop + LINE_WIDTH, whitePaint)
        }

        // 绘制边框
        canvas.drawRect(l, t, r, t + BORDER_WIDTH, whitePaint)
        canvas.drawRect(l, t, l + BORDER_WIDTH, b, whitePaint)
        canvas.drawRect(r - BORDER_WIDTH, t, r, b, whitePaint)
        canvas.drawRect(l, b - BORDER_WIDTH, r, b, whitePaint)

        // 绘制边角
        canvas.drawRect(l - CORNER_WIDTH, t, l, t + CORNER_LENGTH, whitePaint)
        canvas.drawRect(l - CORNER_WIDTH, t - CORNER_WIDTH, l + CORNER_LENGTH, t, whitePaint)
        canvas.drawRect(r, t, r + CORNER_WIDTH, t + CORNER_LENGTH, whitePaint)
        canvas.drawRect(r - CORNER_LENGTH, t - CORNER_WIDTH, r + CORNER_WIDTH, t, whitePaint)
        canvas.drawRect(l - CORNER_WIDTH, b - CORNER_LENGTH, l, b, whitePaint)
        canvas.drawRect(l - CORNER_WIDTH, b, l + CORNER_LENGTH, b + CORNER_WIDTH, whitePaint)
        canvas.drawRect(r, b - CORNER_LENGTH, r + CORNER_WIDTH, b, whitePaint)
        canvas.drawRect(r - CORNER_LENGTH, b, r + CORNER_WIDTH, b + CORNER_WIDTH, whitePaint)
    }
}

@Composable
fun PictureClipCanvas(
    state: PictureClipState,
    modifier: Modifier = Modifier,
) {
    var dragState by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.viewSize = it }
            .pointerInput(Unit) {
                var prevSpan = 0f
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        if (changes.size >= 2 && changes.all { it.pressed }) {
                            // 双指缩放
                            val p0 = changes[0]
                            val p1 = changes[1]
                            val span = hypot(
                                (p1.position.x - p0.position.x).toDouble(),
                                (p1.position.y - p0.position.y).toDouble()
                            ).toFloat()
                            if (prevSpan > 0f && span > 0f) {
                                val scaleFactor = span / prevSpan
                                val focusX = (p0.position.x + p1.position.x) / 2f
                                val focusY = (p0.position.y + p1.position.y) / 2f
                                state.handleScale(scaleFactor, focusX, focusY)
                            }
                            prevSpan = span
                            if (!dragState) {
                                dragState = true
                            }
                            changes.forEach { it.consume() }
                        } else if (changes.any { it.pressed } && !dragState) {
                            // 手指按下：检测是否在裁剪边框上
                            dragState = true
                            state.handleTouchDown(changes.first().position.x, changes.first().position.y)
                            changes.forEach { it.consume() }
                        } else if (dragState && changes.all { !it.pressed }) {
                            // 手指全部抬起
                            dragState = false
                            prevSpan = 0f
                            state.handleTouchUp()
                            state.finishScaling()
                            changes.forEach { it.consume() }
                        } else if (dragState && changes.size == 1 && changes.all { it.pressed }) {
                            // 单指移动：拖动裁剪边框或平移图片
                            state.handleTouchMove(changes.first().position.x, changes.first().position.y)
                            changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        state.refreshTrigger
        state.draw(drawContext.canvas)
    }
}