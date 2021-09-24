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
        private const val LINE_WIDTH = 2f
        private const val BORDER_WIDTH = 5f
        private const val CORNER_WIDTH = 10f
        private const val CORNER_LENGTH = 50f
        private const val CORNER_MARGIN = 60f
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

    private val clipMatrix = Matrix()
    private val clipRectF = RectF()
    private val maxClipRectF = RectF()

    private val leftTouchRectF = RectF()
    private val topTouchRectF = RectF()
    private val rightTouchRectF = RectF()
    private val bottomTouchRectF = RectF()

    private var isDragBitmap = false
    private var isDragTouchRectF = false

    private var downX = 0f
    private var downY = 0f
    private var currScale = 0f
    private var currTranslateX = 0f
    private var currTranslateY = 0f

    private val sgListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            if (currScale * scaleFactor <= MINIMUM_SCALE) {
                scaleFactor = MINIMUM_SCALE / currScale
            }
            if (currScale * scaleFactor >= MAXIMUM_SCALE) {
                scaleFactor = MAXIMUM_SCALE / currScale
            }
            if (bitmapRectF.height() < clipRectF.height()) {
                scaleFactor = clipRectF.height() / bitmapRectF.height()
            }
            if (currScale > MINIMUM_SCALE && currScale < MAXIMUM_SCALE) {
                clipMatrix.setScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                clipMatrix.mapRect(bitmapRectF)
            }
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
        this.bitmap = orgBitmap
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        clipMatrix.reset()
        currScale = maxClipRectF.width() / bitmapWidth
        updateBitmapRectF()
        updateClipRectF()
        updateClipBorder()
        invalidate()
    }

    fun rotate() {
        clipMatrix.reset()
        clipMatrix.setRotate(-90f, bitmapRectF.centerX(), bitmapRectF.centerY())
        clipMatrix.mapRect(bitmapRectF)
        clipMatrix.mapRect(clipRectF)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, clipMatrix, true)
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        currScale = maxClipRectF.width() / bitmapWidth
        val bitmapLeft = (viewWidth - bitmapWidth * currScale) * 0.5f
        val bitmapTop = (viewHeight - bitmapHeight * currScale) * 0.5f
        val bitmapRight = viewWidth - bitmapLeft
        val bitmapBottom = viewHeight - bitmapTop
        bitmapRectF.set(bitmapLeft, bitmapTop, bitmapRight, bitmapBottom)
        clipRectF.set(bitmapRectF)
        if (clipRectF.left < maxClipRectF.left) {
            clipRectF.left = maxClipRectF.left
        }
        if (clipRectF.top < maxClipRectF.top) {
            clipRectF.top = maxClipRectF.top
        }
        if (clipRectF.right > maxClipRectF.right) {
            clipRectF.right = maxClipRectF.right
        }
        if (clipRectF.bottom > maxClipRectF.bottom) {
            clipRectF.bottom = maxClipRectF.bottom
        }
        updateClipBorder()
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
        updateMaxClipRectF()
        updateBitmapRectF()
        updateClipRectF()
        updateTouchRectF()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                var currX = event.x
                var currY = event.y
                if (maxClipRectF.contains(currX, currY)) {
                    if (!isDragBitmap && leftTouchRectF.contains(currX, currY)) {
                        isDragTouchRectF = true
                        if (currX > rightTouchRectF.left) {
                            currX = rightTouchRectF.left
                        }
                        clipRectF.set(currX, clipRectF.top, clipRectF.right, clipRectF.bottom)
                    }
                    if (!isDragBitmap && topTouchRectF.contains(currX, currY)) {
                        isDragTouchRectF = true
                        if (currY > bottomTouchRectF.top) {
                            currY = bottomTouchRectF.top
                        }
                        clipRectF.set(clipRectF.left, currY, clipRectF.right, clipRectF.bottom)
                    }
                    if (!isDragBitmap && rightTouchRectF.contains(currX, currY)) {
                        isDragTouchRectF = true
                        if (currX < leftTouchRectF.right) {
                            currX = leftTouchRectF.right
                        }
                        clipRectF.set(clipRectF.left, clipRectF.top, currX, clipRectF.bottom)
                    }
                    if (!isDragBitmap && bottomTouchRectF.contains(currX, currY)) {
                        isDragTouchRectF = true
                        if (currY < topTouchRectF.bottom) {
                            currY = topTouchRectF.bottom
                        }
                        clipRectF.set(clipRectF.left, clipRectF.top, clipRectF.right, currY)
                    }
                    if (!isDragTouchRectF) {
                        isDragBitmap = true
                        currTranslateX = currX - downX
                        currTranslateY = currY - downY
                        downX = currX
                        downY = currY
                    } else {
                        currTranslateX = 0f
                        currTranslateY = 0f
                        bitmapFollowBorder()
                    }
                    updateTouchRectF()
                }
            }
            MotionEvent.ACTION_UP -> {
                downX = 0f
                downY = 0f
                isDragBitmap = false
                isDragTouchRectF = false
                bitmapFollowBorder()
                borderCenter()
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        bitmapRectF.offset(currTranslateX, currTranslateY)
        canvas.drawBitmap(bitmap, null, bitmapRectF, null)
        canvas.restore()
        drawExterior(canvas, clipRectF)
        drawLine(canvas, clipRectF)
        drawBorder(canvas, clipRectF)
        drawCorner(canvas, clipRectF)
    }

    private fun updateMaxClipRectF() {
        val maxClipLift = CORNER_MARGIN
        val maxClipTop = CORNER_MARGIN
        val maxClipRight = viewWidth - CORNER_MARGIN
        val maxClipBottom = viewHeight - CORNER_MARGIN
        maxClipRectF.set(maxClipLift, maxClipTop, maxClipRight, maxClipBottom)
        currScale = maxClipRectF.width() / bitmapWidth
    }

    private fun updateClipRectF() {
        clipRectF.set(bitmapRectF)
        if (clipRectF.left < maxClipRectF.left) {
            clipRectF.left = maxClipRectF.left
        }
        if (clipRectF.top < maxClipRectF.top) {
            clipRectF.top = maxClipRectF.top
        }
        if (clipRectF.right > maxClipRectF.right) {
            clipRectF.right = maxClipRectF.right
        }
        if (clipRectF.bottom > maxClipRectF.bottom) {
            clipRectF.bottom = maxClipRectF.bottom
        }
    }

    private fun updateBitmapRectF() {
        val bitmapLeft = (viewWidth - bitmapWidth * currScale) * 0.5f
        val bitmapTop = (viewHeight - bitmapHeight * currScale) * 0.5f
        val bitmapRight = viewWidth - bitmapLeft
        val bitmapBottom = viewHeight - bitmapTop
        bitmapRectF.set(bitmapLeft, bitmapTop, bitmapRight, bitmapBottom)
    }

    private fun updateTouchRectF() {
        val left = clipRectF.left - CORNER_LENGTH
        val top = clipRectF.top - CORNER_LENGTH
        val right = clipRectF.right + CORNER_LENGTH
        val bottom = clipRectF.bottom + CORNER_LENGTH
        leftTouchRectF.set(left, top, clipRectF.left + CORNER_LENGTH, bottom)
        topTouchRectF.set(left, top, right, clipRectF.top + CORNER_LENGTH)
        rightTouchRectF.set(clipRectF.right - CORNER_LENGTH, top, right, bottom)
        bottomTouchRectF.set(left, clipRectF.bottom - CORNER_LENGTH, right, bottom)
    }

    private fun updateClipBorder() {
        if (clipRectF.height() < maxClipRectF.height()) {
            val offsetY = (viewHeight - clipRectF.height()) * 0.5f
            if (clipRectF.height() < bitmapRectF.height()) {
                currTranslateY += offsetY - clipRectF.top
            }
            clipRectF.offset(0f, offsetY - clipRectF.top)
        }
        if (bitmapRectF.left >= clipRectF.left && bitmapRectF.right >= clipRectF.right) {
            currTranslateX = clipRectF.left - bitmapRectF.left
        }
        if (bitmapRectF.left < clipRectF.left && bitmapRectF.right < clipRectF.right) {
            currTranslateX = clipRectF.right - bitmapRectF.right
        }
        if (bitmapRectF.top >= clipRectF.top && bitmapRectF.bottom >= clipRectF.bottom) {
            currTranslateY = clipRectF.top - bitmapRectF.top
        }
        if (bitmapRectF.top < clipRectF.top && bitmapRectF.bottom < clipRectF.bottom) {
            currTranslateY = clipRectF.bottom - bitmapRectF.bottom
        }
    }

    private fun bitmapFollowBorder() {
        if (bitmapRectF.height() < clipRectF.height()) {
            val scaleFactor = clipRectF.height() / bitmapRectF.height()
            val px = clipRectF.centerX() - bitmapRectF.left.coerceAtLeast(0f)
            val py = bitmapRectF.centerY()
            clipMatrix.setScale(scaleFactor, scaleFactor, px, py)
            clipMatrix.mapRect(bitmapRectF)
            currScale *= scaleFactor
            updateClipBorder()
        }
    }

    private fun borderCenter() {
        if (clipRectF.width() < bitmapRectF.width() && clipRectF.width() < maxClipRectF.width()) {
            val offsetX = (viewWidth - clipRectF.width()) * 0.5f
            if (clipRectF.width() < bitmapRectF.width()) {
                currTranslateX += offsetX - clipRectF.left
            }
            clipRectF.offset(offsetX - clipRectF.left, 0f)
            val a = maxClipRectF.height() / clipRectF.height()
            val b = maxClipRectF.width() / clipRectF.width()
            val minScale = min(a, b)
            clipMatrix.setScale(minScale, minScale, clipRectF.centerX(), clipRectF.centerY())
            clipMatrix.mapRect(bitmapRectF)
            clipMatrix.mapRect(clipRectF)
            currScale *= minScale
            if (clipRectF.left < maxClipRectF.left) {
                clipRectF.left = maxClipRectF.left
            }
            if (clipRectF.top < maxClipRectF.top) {
                clipRectF.top = maxClipRectF.top
            }
            if (clipRectF.right > maxClipRectF.right) {
                clipRectF.right = maxClipRectF.right
            }
            if (clipRectF.bottom > maxClipRectF.bottom) {
                clipRectF.bottom = maxClipRectF.bottom
            }
            updateTouchRectF()
        }
    }

    /**
     * 绘制模糊区域
     *
     * @param canvas
     * @param rectF
     */
    private fun drawExterior(canvas: Canvas, rectF: RectF) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        paint.color = Color.parseColor("#60000000")
        canvas.drawRect(0f, 0f, width, rectF.top, paint)
        canvas.drawRect(0f, rectF.top, rectF.left, rectF.bottom, paint)
        canvas.drawRect(rectF.right, rectF.top, width, rectF.bottom, paint)
        canvas.drawRect(0f, rectF.bottom, width, height, paint)
    }

    /**
     * 绘制格线
     *
     * @param canvas
     * @param rectF
     */
    private fun drawLine(canvas: Canvas, rectF: RectF) {
        val width = rectF.width()
        val height = rectF.height()
        paint.color = Color.WHITE
        canvas.drawRect(
            rectF.left + width / 3,
            rectF.top,
            rectF.left + width / 3 + LINE_WIDTH,
            rectF.bottom,
            paint
        )
        canvas.drawRect(
            rectF.left + width / 3 * 2,
            rectF.top,
            rectF.left + width / 3 * 2 + LINE_WIDTH,
            rectF.bottom,
            paint
        )
        canvas.drawRect(
            rectF.left,
            rectF.top + height / 3,
            rectF.right,
            rectF.top + height / 3 + LINE_WIDTH,
            paint
        )
        canvas.drawRect(
            rectF.left,
            rectF.top + height / 3 * 2,
            rectF.right,
            rectF.top + height / 3 * 2 + LINE_WIDTH,
            paint
        )
    }

    /**
     * 绘制边框
     *
     * @param canvas
     * @param rectF
     */
    private fun drawBorder(canvas: Canvas, rectF: RectF) {
        paint.color = Color.WHITE
        canvas.drawRect(rectF.left, rectF.top, rectF.right, rectF.top + BORDER_WIDTH, paint)
        canvas.drawRect(rectF.left, rectF.top, rectF.left + BORDER_WIDTH, rectF.bottom, paint)
        canvas.drawRect(rectF.right - BORDER_WIDTH, rectF.top, rectF.right, rectF.bottom, paint)
        canvas.drawRect(rectF.left, rectF.bottom - BORDER_WIDTH, rectF.right, rectF.bottom, paint)
    }

    /**
     * 绘制边角
     *
     * @param canvas
     * @param rectF
     */
    private fun drawCorner(canvas: Canvas, rectF: RectF) {
        paint.color = Color.WHITE
        //左上
        canvas.drawRect(
            rectF.left - CORNER_WIDTH,
            rectF.top,
            rectF.left,
            rectF.top + CORNER_LENGTH,
            paint
        )
        canvas.drawRect(
            rectF.left - CORNER_WIDTH,
            rectF.top - CORNER_WIDTH,
            rectF.left + CORNER_LENGTH,
            rectF.top,
            paint
        )
        //右上
        canvas.drawRect(
            rectF.right,
            rectF.top,
            rectF.right + CORNER_WIDTH,
            rectF.top + CORNER_LENGTH,
            paint
        )
        canvas.drawRect(
            rectF.right - CORNER_LENGTH,
            rectF.top - CORNER_WIDTH,
            rectF.right + CORNER_WIDTH,
            rectF.top,
            paint
        )
        //左下
        canvas.drawRect(
            rectF.left - CORNER_WIDTH,
            rectF.bottom - CORNER_LENGTH,
            rectF.left,
            rectF.bottom,
            paint
        )
        canvas.drawRect(
            rectF.left - CORNER_WIDTH,
            rectF.bottom,
            rectF.left + CORNER_LENGTH,
            rectF.bottom + CORNER_WIDTH,
            paint
        )
        //右下
        canvas.drawRect(
            rectF.right,
            rectF.bottom - CORNER_LENGTH,
            rectF.right + CORNER_WIDTH,
            rectF.bottom,
            paint
        )
        canvas.drawRect(
            rectF.right - CORNER_LENGTH,
            rectF.bottom,
            rectF.right + CORNER_WIDTH,
            rectF.bottom + CORNER_WIDTH,
            paint
        )
    }

}