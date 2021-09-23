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

        private const val MINIMUM_SCALE = 0.1f
        private const val MAXIMUM_SCALE = 2.5f
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private val bitmapRectF = RectF()
    private var orgBitmap: Bitmap? = null
    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val clipMatrix = Matrix()
    private val maxClipRectF = RectF()
    private val clipRectF = RectF()
    private val leftTouchRectF = RectF()
    private val topTouchRectF = RectF()
    private val rightTouchRectF = RectF()
    private val bottomTouchRectF = RectF()

    private var isDragBitmap = false
    private var isDragTouchRectF = false

    private var downX = 0f
    private var downY = 0f
    private var currTranslateX = 0f
    private var currTranslateY = 0f
    private var currScale = 0f
    private val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

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
    )

    fun setClipBitmapResource(bitmap: Bitmap) {
        this.orgBitmap = bitmap
        this.bitmap = bitmap
        bitmap.let {
            bitmapWidth = it.width
            bitmapHeight = it.height
        }
        invalidate()
    }

    fun reset() {
        bitmap = orgBitmap
        bitmap?.let {
            bitmapWidth = it.width
            bitmapHeight = it.height
        }
        clipMatrix.reset()
        currScale = maxClipRectF.width() / bitmapWidth
        val leftOffset = (viewWidth - bitmapWidth * currScale) * 0.5f
        val topOffset = (viewHeight - bitmapHeight * currScale) * 0.5f
        bitmapRectF.set(
            leftOffset,
            topOffset,
            viewWidth - leftOffset,
            viewHeight - topOffset
        )
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
        measureTouchRectF()
        rectFBorder()
        invalidate()
    }

    fun rotate() {
        clipMatrix.reset()
        clipMatrix.setRotate(-90f, bitmapRectF.centerX(), bitmapRectF.centerY())
        clipMatrix.mapRect(bitmapRectF)
        clipMatrix.mapRect(clipRectF)
        bitmap?.let {
            bitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, clipMatrix, true)
        }
        bitmap?.let {
            bitmapWidth = it.width
            bitmapHeight = it.height
        }
        currScale = maxClipRectF.width() / bitmapWidth
        val leftOffset = (viewWidth - bitmapWidth * currScale) * 0.5f
        val topOffset = (viewHeight - bitmapHeight * currScale) * 0.5f
        bitmapRectF.set(
            leftOffset,
            topOffset,
            viewWidth - leftOffset,
            viewHeight - topOffset
        )
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
        measureTouchRectF()
        rectFBorder()
        invalidate()
    }

    fun saveBitmap(): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isAntiAlias = true
        paint.isDither = true
        paint.isFilterBitmap = true
        val clipBitmap = Bitmap.createBitmap(
            clipRectF.width().toInt(),
            clipRectF.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(clipBitmap)
        val left = -clipRectF.left
        val top = -clipRectF.top
        clipRectF.offset(left, top)
        canvas.drawRect(clipRectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        bitmap?.let {
            bitmapRectF.offset(left, top)
            canvas.drawBitmap(it, null, bitmapRectF, paint)
        }
        return clipBitmap
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        val corner = CORNER_WIDTH + CORNER_LENGTH
        maxClipRectF.set(corner, corner, viewWidth - corner, viewHeight - corner)
        currScale = maxClipRectF.width() / bitmapWidth
        val leftOffset = (viewWidth - bitmapWidth * currScale) * 0.5f
        val topOffset = (viewHeight - bitmapHeight * currScale) * 0.5f
        bitmapRectF.set(
            leftOffset,
            topOffset,
            viewWidth - leftOffset,
            viewHeight - topOffset
        )
        clipRectF.set(bitmapRectF)
        if (clipRectF.top < maxClipRectF.top) {
            clipRectF.top = maxClipRectF.top
        }
        if (clipRectF.bottom > maxClipRectF.bottom) {
            clipRectF.bottom = maxClipRectF.bottom
        }
        measureTouchRectF()
    }

    private fun measureTouchRectF() {
        leftTouchRectF.set(
            clipRectF.left - CORNER_LENGTH,
            clipRectF.top - CORNER_LENGTH,
            clipRectF.left + CORNER_LENGTH,
            clipRectF.bottom + CORNER_LENGTH
        )
        topTouchRectF.set(
            clipRectF.left - CORNER_LENGTH,
            clipRectF.top - CORNER_LENGTH,
            clipRectF.right + CORNER_LENGTH,
            clipRectF.top + CORNER_LENGTH
        )
        rightTouchRectF.set(
            clipRectF.right - CORNER_LENGTH,
            clipRectF.top - CORNER_LENGTH,
            clipRectF.right + CORNER_LENGTH,
            clipRectF.bottom + CORNER_LENGTH
        )
        bottomTouchRectF.set(
            clipRectF.left - CORNER_LENGTH,
            clipRectF.bottom - CORNER_LENGTH,
            clipRectF.right + CORNER_LENGTH,
            clipRectF.bottom + CORNER_LENGTH
        )
    }

    private fun rectFBorder() {
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
                    measureTouchRectF()
                }
                if (!isDragTouchRectF) {
                    isDragBitmap = true
                    currTranslateX = currX - downX
                    currTranslateY = currY - downY
                    if (downX == 0f) {
                        currTranslateX = 0f
                    }
                    if (downY == 0f) {
                        currTranslateY = 0f
                    }
                    downX = currX
                    downY = currY
                } else {
                    currTranslateX = 0f
                    currTranslateY = 0f
                    if (bitmapRectF.height() < clipRectF.height()) {
                        val scaleFactor = clipRectF.height() / bitmapRectF.height()
                        clipMatrix.setScale(
                            scaleFactor,
                            scaleFactor,
                            bitmapRectF.centerX(),
                            bitmapRectF.centerY()
                        )
                        clipMatrix.mapRect(bitmapRectF)
                        currScale *= scaleFactor
                        rectFBorder()
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                downX = 0f
                downY = 0f
                isDragBitmap = false
                isDragTouchRectF = false
                if (bitmapRectF.height() < clipRectF.height()) {
                    val scaleFactor = clipRectF.height() / bitmapRectF.height()
                    clipMatrix.setScale(
                        scaleFactor,
                        scaleFactor,
                        bitmapRectF.centerX(),
                        bitmapRectF.centerY()
                    )
                    clipMatrix.mapRect(bitmapRectF)
                    currScale *= scaleFactor
                }
                if (clipRectF.width() < bitmapRectF.width() && clipRectF.width() < maxClipRectF.width()) {
                    val offsetX = (viewWidth - clipRectF.width()) * 0.5f
                    if (clipRectF.width() < bitmapRectF.width()) {
                        currTranslateX += offsetX - clipRectF.left
                    }
                    clipRectF.offset(offsetX - clipRectF.left, 0f)
                    val a = maxClipRectF.height() / clipRectF.height()
                    val b = maxClipRectF.width() / clipRectF.width()
                    val minScale = min(a, b)
                    clipMatrix.setScale(
                        minScale,
                        minScale,
                        clipRectF.centerX(),
                        clipRectF.centerY()
                    )
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
                    measureTouchRectF()
                }
                rectFBorder()
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            canvas.save()
            bitmapRectF.offset(currTranslateX, currTranslateY)
            canvas.drawBitmap(it, null, bitmapRectF, null)
            canvas.restore()
            drawExterior(canvas, clipRectF)
            drawLine(canvas, clipRectF)
            drawBorder(canvas, clipRectF)
            drawCorner(canvas, clipRectF)
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