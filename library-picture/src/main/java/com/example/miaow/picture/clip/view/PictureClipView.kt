package com.example.miaow.picture.clip.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.values
import kotlin.math.max
import kotlin.math.min

class PictureClipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

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

    private var paddingTop = 0f
    private var paddingBottom = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private lateinit var orgBitmap: Bitmap
    private lateinit var bitmap: Bitmap
    private val bitmapMatrix = Matrix()
    private val bitmapRectF = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val clipRectF = RectF()
    private val maxClipRectF = RectF()

    private val leftDragRectF = RectF()
    private val topDragRectF = RectF()
    private val rightDragRectF = RectF()
    private val bottomDragRectF = RectF()

    private var isLeftDrag = false
    private var isTopDrag = false
    private var isRightDrag = false
    private var isBottomDrag = false

    private var isBitmapDrag = false
    private var isScaling = false

    private var downX = 0f
    private var downY = 0f

    private val sgListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            val currScale = bitmapMatrix.values()[0]
            var scaleFactor = detector.scaleFactor
            if (currScale * scaleFactor <= MINIMUM_SCALE) {
                scaleFactor = MINIMUM_SCALE / currScale
            }
            if (currScale * scaleFactor >= MAXIMUM_SCALE) {
                scaleFactor = MAXIMUM_SCALE / currScale
            }
            bitmapMatrix.setScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            bitmapMatrix.mapRect(bitmapRectF)
            return true
        }

    }
    private val scaleGestureDetector = ScaleGestureDetector(context, sgListener)

    fun setPadding(top: Float, bottom: Float) {
        paddingTop = top
        paddingBottom = bottom
    }

    fun setBitmapResource(bitmap: Bitmap) {
        this.orgBitmap = bitmap
        this.bitmap = bitmap
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        postInvalidate()
    }

    fun reset() {
        bitmap = orgBitmap
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        bitmapMatrix.reset()
        computeBitmapRectF()
        computeDragRectF()
        clipBorderCenter()
        postInvalidate()
    }

    fun rotate() {
        bitmapMatrix.reset()
        bitmapMatrix.setRotate(-90f, clipRectF.centerX(), clipRectF.centerY())
        bitmapMatrix.mapRect(bitmapRectF)
        bitmapMatrix.mapRect(clipRectF)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, bitmapMatrix, true)
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        computeBitmapRectF()
        computeDragRectF()
        clipBorderCenter()
        postInvalidate()
    }

    fun saveBitmap(): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isAntiAlias = true
        paint.isDither = true
        paint.isFilterBitmap = true
        val width = clipRectF.width().toInt()
        val height = clipRectF.height().toInt()
        val clipBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(clipBitmap)
        val left = -clipRectF.left
        val top = -clipRectF.top
        clipRectF.offset(left, top)
        canvas.drawRect(clipRectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        bitmapRectF.offset(left, top)
        canvas.drawBitmap(bitmap, null, bitmapRectF, paint)
        return clipBitmap
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.viewWidth = w
        this.viewHeight = h
        computeBitmapRectF()
        computeDragRectF()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                if (leftDragRectF.contains(event.x, event.y)) {
                    isLeftDrag = true
                    isRightDrag = false
                }
                if (topDragRectF.contains(event.x, event.y)) {
                    isTopDrag = true
                    isBottomDrag = false
                }
                if (rightDragRectF.contains(event.x, event.y)) {
                    isLeftDrag = false
                    isRightDrag = true
                }
                if (bottomDragRectF.contains(event.x, event.y)) {
                    isTopDrag = false
                    isBottomDrag = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && maxClipRectF.contains(event.x, event.y)) {
                    if (!isBitmapDrag) {
                        if (isLeftDrag) {
                            clipRectF.left = min(event.x, rightDragRectF.left)
                        }
                        if (isTopDrag) {
                            clipRectF.top = min(event.y, bottomDragRectF.top)
                        }
                        if (isRightDrag) {
                            clipRectF.right = max(event.x, leftDragRectF.right)
                        }
                        if (isBottomDrag) {
                            clipRectF.bottom = max(event.y, topDragRectF.bottom)
                        }
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
                    if (!isDragging() && bitmapRectF.contains(event.x, event.y)) {
                        isBitmapDrag = true
                        bitmapRectF.offset(event.x - downX, event.y - downY)
                        downX = event.x
                        downY = event.y
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                downX = 0f
                downY = 0f
                resetBitmapRectF()
                clipBorderCenter()
                resetState()
            }
        }
        postInvalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.drawBitmap(bitmap, null, bitmapRectF, null)
        canvas.restore()
        val w = viewWidth.toFloat()
        val h = viewHeight.toFloat()
        val l = clipRectF.left
        val t = clipRectF.top
        val r = clipRectF.right
        val b = clipRectF.bottom
        //绘制模糊区域
        paint.color = Color.parseColor("#60000000")
        canvas.drawRect(0f, 0f, w, t, paint)
        canvas.drawRect(0f, t, l, b, paint)
        canvas.drawRect(r, t, w, b, paint)
        canvas.drawRect(0f, b, w, h, paint)
        paint.color = Color.WHITE
        //绘制格线
        val columnSpacing = clipRectF.width() / 3
        val rowSpacing = clipRectF.height() / 3
        for (i in 1..LINE_NUMBER) {
            val lineLeft = l + columnSpacing * i
            canvas.drawRect(lineLeft, t, lineLeft + LINE_WIDTH, b, paint)
            val lineTop = t + rowSpacing * i
            canvas.drawRect(l, lineTop, r, lineTop + LINE_WIDTH, paint)
        }
        //绘制边框
        canvas.drawRect(l, t, r, t + BORDER_WIDTH, paint)
        canvas.drawRect(l, t, l + BORDER_WIDTH, b, paint)
        canvas.drawRect(r - BORDER_WIDTH, t, r, b, paint)
        canvas.drawRect(l, b - BORDER_WIDTH, r, b, paint)
        //绘制边角
        canvas.drawRect(l - CORNER_WIDTH, t, l, t + CORNER_LENGTH, paint)
        canvas.drawRect(l - CORNER_WIDTH, t - CORNER_WIDTH, l + CORNER_LENGTH, t, paint)
        canvas.drawRect(r, t, r + CORNER_WIDTH, t + CORNER_LENGTH, paint)
        canvas.drawRect(r - CORNER_LENGTH, t - CORNER_WIDTH, r + CORNER_WIDTH, t, paint)
        canvas.drawRect(l - CORNER_WIDTH, b - CORNER_LENGTH, l, b, paint)
        canvas.drawRect(l - CORNER_WIDTH, b, l + CORNER_LENGTH, b + CORNER_WIDTH, paint)
        canvas.drawRect(r, b - CORNER_LENGTH, r + CORNER_WIDTH, b, paint)
        canvas.drawRect(r - CORNER_LENGTH, b, r + CORNER_WIDTH, b + CORNER_WIDTH, paint)
    }

    private fun computeBitmapRectF() {
        val maxClipLift = DRAG_WIDTH
        val maxClipTop = DRAG_WIDTH + paddingTop
        val maxClipRight = viewWidth - DRAG_WIDTH
        val maxClipBottom = viewHeight - DRAG_WIDTH - paddingBottom
        maxClipRectF.set(maxClipLift, maxClipTop, maxClipRight, maxClipBottom)
        val scaleFactor = if (bitmapWidth > bitmapHeight) {
            maxClipRectF.width() / bitmapWidth
        } else {
            maxClipRectF.height() / bitmapHeight
        }
        val bitmapLeft = (viewWidth - bitmapWidth * scaleFactor) * 0.5f
        val bitmapTop = (viewHeight - bitmapHeight * scaleFactor) * 0.5f
        val bitmapRight = viewWidth - bitmapLeft
        val bitmapBottom = viewHeight - bitmapTop
        bitmapRectF.set(bitmapLeft, bitmapTop, bitmapRight, bitmapBottom)
        val clipLift = bitmapRectF.left.coerceAtLeast(maxClipRectF.left)
        val clipTop = bitmapRectF.top.coerceAtLeast(maxClipRectF.top)
        val clipRight = bitmapRectF.right.coerceAtMost(maxClipRectF.right)
        val clipBottom = bitmapRectF.bottom.coerceAtMost(maxClipRectF.bottom)
        clipRectF.set(clipLift, clipTop, clipRight, clipBottom)
    }

    private fun resetBitmapRectF() {
        if (bitmapRectF.width() < clipRectF.width() || bitmapRectF.height() < clipRectF.height()) {
            val a = clipRectF.width() / bitmapRectF.width()
            val b = clipRectF.height() / bitmapRectF.height()
            val scale = max(a, b)
            val px = bitmapRectF.centerX()
            val py = bitmapRectF.centerY()
            bitmapMatrix.setScale(scale, scale, px, py)
            bitmapMatrix.mapRect(bitmapRectF)
        }
        if (bitmapRectF.left > clipRectF.left) {
            bitmapRectF.offset(clipRectF.left - bitmapRectF.left, 0f)
        }
        if (bitmapRectF.top > clipRectF.top) {
            bitmapRectF.offset(0f, clipRectF.top - bitmapRectF.top)
        }
        if (bitmapRectF.right < clipRectF.right) {
            bitmapRectF.offset(clipRectF.right - bitmapRectF.right, 0f)
        }
        if (bitmapRectF.bottom < clipRectF.bottom) {
            bitmapRectF.offset(0f, clipRectF.bottom - bitmapRectF.bottom)
        }
    }

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
        val offsetX = (viewWidth - clipRectF.width()) * 0.5f - clipRectF.left
        val offsetY = (viewHeight - clipRectF.height()) * 0.5f - clipRectF.top
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

    private fun isDragging(): Boolean {
        return isLeftDrag || isTopDrag || isRightDrag || isBottomDrag
    }

}