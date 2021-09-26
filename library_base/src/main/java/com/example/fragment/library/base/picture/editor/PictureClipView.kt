package com.example.fragment.library.base.picture.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
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

    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private lateinit var orgBitmap: Bitmap
    private lateinit var bitmap: Bitmap
    private val bitmapRectF = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val picClipMatrix = Matrix()
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
    private var currScale = 0f
    private var currOffsetX = 0f
    private var currOffsetY = 0f

    private val sgListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            var scaleFactor = detector.scaleFactor
            if (currScale * scaleFactor <= MINIMUM_SCALE) {
                scaleFactor = MINIMUM_SCALE / currScale
            }
            if (currScale * scaleFactor >= MAXIMUM_SCALE) {
                scaleFactor = MAXIMUM_SCALE / currScale
            }
            picClipMatrix.setScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            picClipMatrix.mapRect(bitmapRectF)
            currScale *= scaleFactor
            return true
        }

    }
    private val scaleGestureDetector = ScaleGestureDetector(context, sgListener)

    fun setBitmapResource(bitmap: Bitmap) {
        this.orgBitmap = bitmap
        this.bitmap = bitmap
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        invalidate()
    }

    fun reset() {
        bitmap = orgBitmap
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        picClipMatrix.reset()
        computeMaxClipRectF()
        computeBitmapRectF()
        computeClipRectF()
        computeDragRectF()
        invalidate()
    }

    fun rotate() {
        picClipMatrix.reset()
        picClipMatrix.setRotate(-90f, clipRectF.centerX(), clipRectF.centerY())
        picClipMatrix.mapRect(bitmapRectF)
        picClipMatrix.mapRect(clipRectF)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, picClipMatrix, true)
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        computeClipRectF()
        computeDragRectF()
        borderCenter()
        currScale = maxClipRectF.width() / bitmapWidth
        invalidate()
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
        computeMaxClipRectF()
        computeBitmapRectF()
        computeClipRectF()
        computeDragRectF()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && maxClipRectF.contains(event.x, event.y)) {
                    if (!isBitmapDrag) {
                        updateDragRectF(event.x, event.y)
                    }
                    if (!isDragging()) {
                        updateBitmapRectF(event.x, event.y)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                downX = 0f
                downY = 0f
                currOffsetX = 0f
                currOffsetY = 0f
                resetDragState()
                resetBitmapRectF()
                borderCenter()
            }
        }
        invalidate()
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
        //左上
        canvas.drawRect(l - CORNER_WIDTH, t, l, t + CORNER_LENGTH, paint)
        canvas.drawRect(l - CORNER_WIDTH, t - CORNER_WIDTH, l + CORNER_LENGTH, t, paint)
        //右上
        canvas.drawRect(r, t, r + CORNER_WIDTH, t + CORNER_LENGTH, paint)
        canvas.drawRect(r - CORNER_LENGTH, t - CORNER_WIDTH, r + CORNER_WIDTH, t, paint)
        //左下
        canvas.drawRect(l - CORNER_WIDTH, b - CORNER_LENGTH, l, b, paint)
        canvas.drawRect(l - CORNER_WIDTH, b, l + CORNER_LENGTH, b + CORNER_WIDTH, paint)
        //右下
        canvas.drawRect(r, b - CORNER_LENGTH, r + CORNER_WIDTH, b, paint)
        canvas.drawRect(r - CORNER_LENGTH, b, r + CORNER_WIDTH, b + CORNER_WIDTH, paint)
    }

    private fun computeMaxClipRectF() {
        val maxClipLift = DRAG_WIDTH
        val maxClipTop = DRAG_WIDTH
        val maxClipRight = viewWidth - DRAG_WIDTH
        val maxClipBottom = viewHeight - DRAG_WIDTH
        maxClipRectF.set(maxClipLift, maxClipTop, maxClipRight, maxClipBottom)
        currScale = maxClipRectF.width() / bitmapWidth
    }

    private fun computeClipRectF() {
        val left = bitmapRectF.left.coerceAtLeast(maxClipRectF.left)
        val top = bitmapRectF.top.coerceAtLeast(maxClipRectF.top)
        val right = bitmapRectF.right.coerceAtMost(maxClipRectF.right)
        val bottom = bitmapRectF.bottom.coerceAtMost(maxClipRectF.bottom)
        clipRectF.set(left, top, right, bottom)
    }

    private fun computeBitmapRectF() {
        val left = (viewWidth - bitmapWidth * currScale) * 0.5f
        val top = (viewHeight - bitmapHeight * currScale) * 0.5f
        val right = viewWidth - left
        val bottom = viewHeight - top
        bitmapRectF.set(left, top, right, bottom)
    }

    private fun updateBitmapRectF(x: Float, y: Float) {
        if (bitmapRectF.contains(x, y)) {
            isBitmapDrag = true
            currOffsetX = x - downX
            currOffsetY = y - downY
            bitmapRectF.offset(currOffsetX, currOffsetY)
            downX = x
            downY = y
        }
    }

    private fun resetBitmapRectF() {
        if (bitmapRectF.width() < clipRectF.width()) {
            val scaleFactor = clipRectF.width() / bitmapRectF.width()
            val px = bitmapRectF.centerX()
            val py = bitmapRectF.centerY()
            picClipMatrix.setScale(scaleFactor, scaleFactor, px, py)
            picClipMatrix.mapRect(bitmapRectF)
            currScale *= scaleFactor
        }
        if (bitmapRectF.height() < clipRectF.height()) {
            val scaleFactor = clipRectF.height() / bitmapRectF.height()
            val px = (bitmapRectF.left - DRAG_WIDTH) + clipRectF.centerX()
            val py = bitmapRectF.centerY()
            picClipMatrix.setScale(scaleFactor, scaleFactor, px, py)
            picClipMatrix.mapRect(bitmapRectF)
            currScale *= scaleFactor
        }
        if (bitmapRectF.left > clipRectF.left) {
            currOffsetX = clipRectF.left - bitmapRectF.left
        }
        if (bitmapRectF.right < clipRectF.right) {
            currOffsetX = clipRectF.right - bitmapRectF.right
        }
        if (bitmapRectF.top > clipRectF.top) {
            currOffsetY = clipRectF.top - bitmapRectF.top
        }
        if (bitmapRectF.bottom < clipRectF.bottom) {
            currOffsetY = clipRectF.bottom - bitmapRectF.bottom
        }
        bitmapRectF.offset(currOffsetX, currOffsetY)
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

    private fun updateDragRectF(x: Float, y: Float) {
        if (leftDragRectF.contains(x, y)) {
            isLeftDrag = true
            isRightDrag = false
        }
        if (topDragRectF.contains(x, y)) {
            isTopDrag = true
            isBottomDrag = false
        }
        if (rightDragRectF.contains(x, y)) {
            isLeftDrag = false
            isRightDrag = true
        }
        if (bottomDragRectF.contains(x, y)) {
            isTopDrag = false
            isBottomDrag = true
        }
        if (isLeftDrag) {
            clipRectF.left = x.coerceAtMost(rightDragRectF.left)
        }
        if (isTopDrag) {
            clipRectF.top = y.coerceAtMost(bottomDragRectF.top)
        }
        if (isRightDrag) {
            clipRectF.right = x.coerceAtLeast(leftDragRectF.right)
        }
        if (isBottomDrag) {
            clipRectF.bottom = y.coerceAtLeast(topDragRectF.bottom)
        }
        if (bitmapRectF.height() < clipRectF.height()) {
            val scaleFactor = clipRectF.height() / bitmapRectF.height()
            val px = (bitmapRectF.left - DRAG_WIDTH) + clipRectF.centerX()
            val py = bitmapRectF.centerY()
            picClipMatrix.setScale(scaleFactor, scaleFactor, px, py)
            picClipMatrix.mapRect(bitmapRectF)
            currScale *= scaleFactor
            resetBitmapRectF()
        }
        computeDragRectF()
    }

    private fun resetDragState() {
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

    private fun borderCenter() {
        if (clipRectF.width() < maxClipRectF.width()) {
            val offsetX = (viewWidth - clipRectF.width()) * 0.5f
            if (clipRectF.width() < bitmapRectF.width()) {
                currOffsetX += offsetX - clipRectF.left
            }
            clipRectF.offset(offsetX - clipRectF.left, 0f)
            val a = maxClipRectF.height() / clipRectF.height()
            val b = maxClipRectF.width() / clipRectF.width()
            val minScale = min(a, b)
            picClipMatrix.setScale(minScale, minScale, clipRectF.centerX(), clipRectF.centerY())
            picClipMatrix.mapRect(bitmapRectF)
            picClipMatrix.mapRect(clipRectF)
            currScale *= minScale
            computeDragRectF()
        }
    }
}