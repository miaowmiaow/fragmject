package com.example.fragment.library.base.picture.editor.layer

import android.graphics.*
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.fragment.library.base.picture.utils.Vector2D

class StickerLayer(private val parentView: View) : ILayer {

    companion object {
        private const val INVALID_POINTER_ID = -1
        private const val MINIMUM_SCALE = 0.5f
        private const val MAXIMUM_SCALE = 2.0f
        private const val RECT_ROUND = 30.0f
    }

    private var pointerIndexId0 = INVALID_POINTER_ID
    private var pointerIndexId1 = INVALID_POINTER_ID

    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private val bitmapRectF = RectF()

    private lateinit var bitmap: Bitmap
    private var contentDescription = ""

    private val borderPaint = Paint()
    private val borderRectF = RectF()
    private var inBorder = false
    private var isInProgress = false

    private var downX = 0f
    private var downY = 0f
    private var currTranslateX = 0f
    private var currTranslateY = 0f
    private var initRotation = 0f
    private var currRotation = 0f
    private val prevSpanVector = Vector2D(0f, 0f)
    private val currSpanVector = Vector2D(0f, 0f)

    private var currScale = 1f
    private val scaleGestureDetector = ScaleGestureDetector(parentView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                adjustScale(detector.scaleFactor)
                parentView.invalidate()
                return true
            }

        })

    private var onClickListener: OnClickListener? = null
    private var touchTime = 0L
    var parentTouchX = 0f
    var parentTouchY = 0f
    var isEnabled = true

    init {
        borderPaint.isAntiAlias = true
        borderPaint.color = Color.WHITE
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 3f
    }

    fun setBitmap(
        bitmap: Bitmap,
        contentDescription: String,
        parentTouchX: Float,
        parentTouchY: Float
    ) {
        this.bitmap = bitmap
        this.contentDescription = contentDescription
        this.bitmapWidth = bitmap.width
        this.bitmapHeight = bitmap.height
        currTranslateX = if (parentTouchX > 0) parentTouchX else viewWidth * 0.5f
        currTranslateY = if (parentTouchY > 0) parentTouchY else viewHeight * 0.5f
        measureBitmap()
        parentView.invalidate()
    }

    fun inTextBounds(x: Float, y: Float): Boolean {
        return borderRectF.contains(x, y)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    private fun measureBitmap() {
        bitmapRectF.set(
            currTranslateX - (bitmapWidth * currScale * 0.5f),
            currTranslateY - (bitmapHeight * currScale * 0.5f),
            currTranslateX + (bitmapWidth * currScale * 0.5f),
            currTranslateY + (bitmapHeight * currScale * 0.5f),
        )
        borderRectF.set(
            bitmapRectF.left - RECT_ROUND,
            bitmapRectF.top - RECT_ROUND,
            bitmapRectF.right + RECT_ROUND,
            bitmapRectF.bottom + RECT_ROUND
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            scaleGestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchTime = System.currentTimeMillis()
                    isInProgress = false
                    val pointerId0 = event.getPointerId(0)
                    pointerIndexId0 = event.findPointerIndex(pointerId0)
                    downX = event.x
                    downY = event.y
                    if (!inBorder) {
                        inBorder = true
                        parentView.invalidate()
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    isInProgress = true
                    val pointerId1 = event.getPointerId(event.actionIndex)
                    pointerIndexId1 = event.findPointerIndex(pointerId1)
                    computeSpanVector(event)
                    prevSpanVector.set(currSpanVector)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (pointerIndexId0 != INVALID_POINTER_ID) {
                        if (pointerIndexId1 != INVALID_POINTER_ID) {
                            computeSpanVector(event)
                            val deltaAngle = Vector2D.getAngle(prevSpanVector, currSpanVector)
                            adjustAngle(deltaAngle)
                        } else {
                            if (!isInProgress) {
                                val currX = event.getX(pointerIndexId0)
                                val currY = event.getY(pointerIndexId0)
                                adjustTranslation(currX - downX, currY - downY)
                                downX = currX
                                downY = currY
                            }
                        }
                        parentView.invalidate()
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    pointerIndexId1 = if (event.pointerCount > 2) {
                        val pointerId1 = event.getPointerId(event.actionIndex)
                        event.findPointerIndex(pointerId1)
                    } else {
                        INVALID_POINTER_ID
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val time = System.currentTimeMillis() - touchTime
                    if (time < 300L) {
                        onClickListener?.onClick(
                            bitmap,
                            contentDescription,
                            parentTouchX,
                            parentTouchY
                        )
                    }
                    pointerIndexId0 = INVALID_POINTER_ID
                    pointerIndexId1 = INVALID_POINTER_ID
                    initRotation = currRotation
                    if (inBorder) {
                        inBorder = false
                        parentView.invalidate()
                    }
                    return false
                }
            }
        }
        return isEnabled
    }

    private fun adjustAngle(degrees: Float) {
        var rotation = degrees
        if (rotation > 180.0f) {
            rotation -= 360.0f
        } else if (rotation < -180.0f) {
            rotation += 360.0f
        }
        if (!degrees.isNaN()) {
            currRotation = initRotation + degrees
        }
    }

    private fun computeSpanVector(event: MotionEvent) {
        if (pointerIndexId0 != INVALID_POINTER_ID && pointerIndexId1 != INVALID_POINTER_ID) {
            val cx0 = event.getX(pointerIndexId0)
            val cy0 = event.getY(pointerIndexId0)
            val cx1 = event.getX(pointerIndexId1)
            val cy1 = event.getY(pointerIndexId1)
            val cvx = cx1 - cx0
            val cvy = cy1 - cy0
            if (!cvx.isNaN() && !cvy.isNaN()) {
                currSpanVector.set(cvx, cvy)
            }
        }
    }

    private fun adjustTranslation(deltaX: Float, deltaY: Float) {
        currTranslateX += deltaX
        currTranslateY += deltaY
    }

    private fun adjustScale(deltaScale: Float) {
        currScale *= deltaScale
        currScale = MINIMUM_SCALE.coerceAtLeast(MAXIMUM_SCALE.coerceAtMost(currScale))
    }

    override fun onSizeChanged(w: Int, h: Int) {
        this.viewWidth = w
        this.viewHeight = h
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvasRotate(canvas)
        drawBorder(canvas)
        drawBitmap(canvas)
        canvas.restore()
    }

    private fun canvasRotate(canvas: Canvas) {
        canvas.rotate(currRotation, borderRectF.centerX(), borderRectF.centerY())
    }

    private fun drawBorder(canvas: Canvas) {
        if (inBorder) {
            canvas.drawRect(borderRectF, borderPaint)
        }
    }

    private fun drawBitmap(canvas: Canvas) {
        measureBitmap()
        canvas.drawBitmap(bitmap, null, bitmapRectF, null)
    }

    interface OnClickListener {
        fun onClick(
            bitmap: Bitmap?,
            contentDescription: String,
            parentTouchX: Float,
            parentTouchY: Float
        )
    }

}