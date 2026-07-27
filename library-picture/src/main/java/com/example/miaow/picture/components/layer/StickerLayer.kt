package com.example.miaow.picture.components.layer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.graphics.values
import androidx.core.graphics.withRotation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import androidx.core.graphics.withSave
import com.example.miaow.picture.components.PictureEditorState
import com.example.miaow.picture.data.StickerAttrs

class StickerLayer(
    private val state: PictureEditorState,
    private val attrs: StickerAttrs,
    private val listener: OnStickerClickListener? = null
) : ILayer {

    companion object {
        private const val INVALID_POINTER_ID = -1
        private const val MINIMUM_SCALE = 0.2f
        private const val MAXIMUM_SCALE = 6f
        private const val RECT_ROUND = 30.0f
        private const val HANDLE_RADIUS = 24f
        private const val HANDLE_MARGIN = 20f
    }

    private enum class TouchTarget {
        NONE,
        BODY,
        TRANSFORM,
        FLIP,
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private val parentMatrix = Matrix()
    private val stickerRectF = RectF()
    private var stickerWidth = attrs.bitmap.width
    private var stickerHeight = attrs.bitmap.height
    private var currRotation = attrs.rotation
    private var currScale = attrs.scale
    private var currTranslateX = attrs.translateX
    private var currTranslateY = attrs.translateY
    private var isFlipped = attrs.isFlipped
    private var pointerIndexId0 = INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private val borderPaint = Paint()
    private val borderRectF = RectF()
    private val bodyHitRectF = RectF()
    private val flipHandleRectF = RectF()
    private val transformHandleRectF = RectF()
    private val handlePaint = Paint()
    private val handleIconPaint = Paint()
    private val handleTextPaint = Paint()
    private var inBorder = false
    private var touchTime = 0L
    private var touchTarget = TouchTarget.NONE
    private var showDeleteBin = false
    private var transformStartDistance = 0f
    private var transformStartAngle = 0f

    var isEnabled = true

    init {
        borderPaint.isAntiAlias = true
        borderPaint.color = Color.WHITE
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 1f

        handlePaint.isAntiAlias = true
        handlePaint.color = Color.WHITE
        handlePaint.style = Paint.Style.FILL

        handleIconPaint.isAntiAlias = true
        handleIconPaint.color = Color.BLACK
        handleIconPaint.style = Paint.Style.FILL

        handleTextPaint.isAntiAlias = true
        handleTextPaint.color = Color.BLACK
        handleTextPaint.textAlign = Paint.Align.CENTER
        handleTextPaint.textSize = 24f
        handleTextPaint.isFakeBoldText = true
    }

    fun updateParentMatrix(matrix: Matrix) {
        parentMatrix.set(matrix)
        measureBitmap()
    }

    fun setSelected(selected: Boolean) {
        val changed = inBorder != selected
        inBorder = selected
        if (!selected) {
            touchTarget = TouchTarget.NONE
            showDeleteBin = false
            pointerIndexId0 = INVALID_POINTER_ID
        }
        if (changed) {
            state.invalidate()
        }
    }

    fun isSelected(): Boolean = inBorder

    fun inStickerBounds(x: Float, y: Float): Boolean {
        return inRectWithRotation(x, y, borderRectF) ||
                inRectWithRotation(x, y, flipHandleRectF) ||
                inRectWithRotation(x, y, transformHandleRectF)
    }

    fun shouldShowDeleteBin(): Boolean = showDeleteBin

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchTime = System.currentTimeMillis()
                val pointerId0 = event.getPointerId(0)
                pointerIndexId0 = event.findPointerIndex(pointerId0)
                downX = event.x
                downY = event.y
                touchTarget = resolveTouchTarget(event.x, event.y)
                showDeleteBin = touchTarget == TouchTarget.BODY
                if (!inBorder) {
                    inBorder = true
                    state.invalidate()
                }
                if (touchTarget == TouchTarget.TRANSFORM) {
                    transformStartDistance = distanceToCenter(event.x, event.y)
                    transformStartAngle = angleToCenter(event.x, event.y)
                }
                return touchTarget != TouchTarget.NONE
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerIndexId0 == INVALID_POINTER_ID) return false
                val currX = event.getX(pointerIndexId0)
                val currY = event.getY(pointerIndexId0)
                when (touchTarget) {
                    TouchTarget.BODY -> {
                        adjustTranslation(currX - downX, currY - downY)
                        downX = currX
                        downY = currY
                    }

                    TouchTarget.TRANSFORM -> {
                        updateTransform(currX, currY)
                    }

                    else -> Unit
                }
                measureBitmap()
                state.invalidate()
                return touchTarget != TouchTarget.NONE
            }

            MotionEvent.ACTION_UP -> {
                val isClick = System.currentTimeMillis() - touchTime < 250
                when (touchTarget) {
                    TouchTarget.FLIP -> {
                        toggleFlip()
                    }

                    TouchTarget.BODY -> {
                        if (isClick && attrs.description.isNotBlank()) {
                            listener?.onClick(
                                StickerAttrs(
                                    bitmap = attrs.bitmap,
                                    description = attrs.description,
                                    rotation = currRotation,
                                    scale = currScale,
                                    translateX = currTranslateX,
                                    translateY = currTranslateY,
                                    isFlipped = isFlipped,
                                )
                            )
                        }
                    }

                    else -> Unit
                }
                if (currTranslateX == 0f) {
                    currTranslateX = defaultCenterX()
                }
                if (currTranslateY == 0f) {
                    currTranslateY = defaultCenterY()
                }
                clampToViewBounds()
                measureBitmap()
                pointerIndexId0 = INVALID_POINTER_ID
                touchTarget = TouchTarget.NONE
                showDeleteBin = false
                state.invalidate()
                return false
            }
        }
        return false
    }

    override fun onSizeChanged(
        viewWidth: Int,
        viewHeight: Int,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight
        this.bitmapWidth = bitmapWidth
        this.bitmapHeight = bitmapHeight
        if (currTranslateX == 0f) {
            currTranslateX = defaultCenterX()
        }
        if (currTranslateY == 0f) {
            currTranslateY = defaultCenterY()
        }
        clampToViewBounds()
        measureBitmap()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.withRotation(currRotation, borderRectF.centerX(), borderRectF.centerY()) {
            if (inBorder) {
                drawRect(borderRectF, borderPaint)
                handlePaint.color = Color.WHITE
                drawCircle(
                    flipHandleRectF.centerX(),
                    flipHandleRectF.centerY(),
                    HANDLE_RADIUS,
                    handlePaint
                )
                handlePaint.color = Color.WHITE
                drawCircle(
                    transformHandleRectF.centerX(),
                    transformHandleRectF.centerY(),
                    HANDLE_RADIUS,
                    handlePaint
                )
                drawHandleLabel(this, flipHandleRectF, "翻")
                drawHandleLabel(this, transformHandleRectF, "旋")
            }
            withSave {
                scale(
                    if (isFlipped) -1f else 1f,
                    1f,
                    stickerRectF.centerX(),
                    stickerRectF.centerY()
                )
                drawBitmap(attrs.bitmap, null, stickerRectF, null)
            }
        }
    }

    private fun drawHandleLabel(canvas: Canvas, rect: RectF, text: String) {
        canvas.drawText(
            text,
            rect.centerX(),
            rect.centerY() + handleTextPaint.textSize / 3f,
            handleTextPaint
        )
    }

    private fun measureBitmap() {
        val halfWidth = stickerWidth * currScale / parentScaleX() * 0.5f
        val halfHeight = stickerHeight * currScale / parentScaleY() * 0.5f
        val stickerLeft = currTranslateX - halfWidth
        val stickerTop = currTranslateY - halfHeight
        val stickerRight = currTranslateX + halfWidth
        val stickerBottom = currTranslateY + halfHeight
        stickerRectF.set(stickerLeft, stickerTop, stickerRight, stickerBottom)

        val borderLeft = stickerRectF.left - RECT_ROUND
        val borderTop = stickerRectF.top - RECT_ROUND
        val borderRight = stickerRectF.right + RECT_ROUND
        val borderBottom = stickerRectF.bottom + RECT_ROUND
        borderRectF.set(borderLeft, borderTop, borderRight, borderBottom)
        bodyHitRectF.set(borderRectF)

        val handleInset = HANDLE_RADIUS + HANDLE_MARGIN
        flipHandleRectF.set(
            borderRectF.left - handleInset,
            borderRectF.top - handleInset,
            borderRectF.left + handleInset,
            borderRectF.top + handleInset,
        )
        transformHandleRectF.set(
            borderRectF.right - handleInset,
            borderRectF.bottom - handleInset,
            borderRectF.right + handleInset,
            borderRectF.bottom + handleInset,
        )
    }

    private fun resolveTouchTarget(x: Float, y: Float): TouchTarget {
        return when {
            inRectWithRotation(x, y, flipHandleRectF) -> TouchTarget.FLIP
            inRectWithRotation(x, y, transformHandleRectF) -> TouchTarget.TRANSFORM
            inRectWithRotation(x, y, bodyHitRectF) -> TouchTarget.BODY
            else -> TouchTarget.NONE
        }
    }

    private fun inRectWithRotation(x: Float, y: Float, rect: RectF): Boolean {
        val mapped = mapPointToUnrotatedSpace(x, y)
        return rect.contains(mapped.first, mapped.second)
    }

    private fun mapPointToUnrotatedSpace(x: Float, y: Float): Pair<Float, Float> {
        val angle = Math.toRadians((-currRotation).toDouble())
        val dx = x - borderRectF.centerX()
        val dy = y - borderRectF.centerY()
        val mappedX = (dx * cos(angle) - dy * sin(angle)).toFloat() + borderRectF.centerX()
        val mappedY = (dx * sin(angle) + dy * cos(angle)).toFloat() + borderRectF.centerY()
        return mappedX to mappedY
    }

    private fun updateTransform(x: Float, y: Float) {
        val distance = distanceToCenter(x, y)
        if (transformStartDistance > 0f && distance > 0f) {
            val scaleFactor = distance / transformStartDistance
            currScale = (currScale * scaleFactor).coerceIn(MINIMUM_SCALE, MAXIMUM_SCALE)
        }
        val angle = angleToCenter(x, y)
        if (transformStartDistance > 0f) {
            currRotation += normalizeAngleDelta(angle - transformStartAngle)
        }
        transformStartDistance = distance
        transformStartAngle = angle
        clampToViewBounds()
    }

    private fun normalizeAngleDelta(delta: Float): Float {
        return when {
            delta > 180f -> delta - 360f
            delta < -180f -> delta + 360f
            else -> delta
        }
    }

    private fun distanceToCenter(x: Float, y: Float): Float {
        return hypot(
            (x - borderRectF.centerX()).toDouble(),
            (y - borderRectF.centerY()).toDouble()
        ).toFloat()
    }

    private fun angleToCenter(x: Float, y: Float): Float {
        return Math.toDegrees(
            atan2(
                (y - borderRectF.centerY()).toDouble(),
                (x - borderRectF.centerX()).toDouble()
            )
        ).toFloat()
    }

    private fun toggleFlip() {
        isFlipped = !isFlipped
        measureBitmap()
        state.invalidate()
    }

    private fun adjustTranslation(deltaX: Float, deltaY: Float) {
        currTranslateX += deltaX
        currTranslateY += deltaY
        clampToViewBounds()
    }

    private fun defaultCenterX(): Float {
        return (viewWidth * 0.5f - parentTranslateX()) / parentScaleX()
    }

    private fun defaultCenterY(): Float {
        val top = -parentTranslateY() / parentScaleY()
        val bottom = (viewHeight - parentTranslateY()) / parentScaleY()
        return (top + bottom) * 0.5f
    }

    private fun clampToViewBounds() {
        val halfWidth = stickerWidth * currScale * 0.5f / parentScaleX()
        val halfHeight = stickerHeight * currScale * 0.5f / parentScaleY()
        val left = -parentTranslateX() / parentScaleX()
        val top = -parentTranslateY() / parentScaleY()
        val right = (viewWidth - parentTranslateX()) / parentScaleX()
        val bottom = (viewHeight - parentTranslateY()) / parentScaleY()

        val minX = left + halfWidth
        val maxX = right - halfWidth
        currTranslateX = if (minX <= maxX) {
            currTranslateX.coerceIn(minX, maxX)
        } else {
            (left + right) * 0.5f
        }

        val minY = top + halfHeight
        val maxY = bottom - halfHeight
        currTranslateY = if (minY <= maxY) {
            currTranslateY.coerceIn(minY, maxY)
        } else {
            (top + bottom) * 0.5f
        }
    }

    private fun parentScaleX(): Float {
        return parentMatrix.values()[0]
    }

    private fun parentScaleY(): Float {
        return parentMatrix.values()[4]
    }

    private fun parentTranslateX(): Float {
        return parentMatrix.values()[2]
    }

    private fun parentTranslateY(): Float {
        return parentMatrix.values()[5]
    }
}

interface OnStickerClickListener {
    fun onClick(attrs: StickerAttrs)
}