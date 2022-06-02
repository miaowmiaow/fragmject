package com.example.miaow.picture.selector.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Scroller
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.values
import kotlin.math.roundToInt

class PhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val imageRectF = RectF()

    private fun getDisplayRectF(): RectF {
        val displayRectF = RectF(imageRectF)
        imageMatrix.mapRect(displayRectF)
        return displayRectF
    }

    private fun currScaleX() = imageMatrix.values()[0]
    private fun currScaleY() = imageMatrix.values()[4]
    private fun currTranslateX() = imageMatrix.values()[2]
    private fun currTranslateY() = imageMatrix.values()[5]
    private fun currImageWidth() = getDisplayRectF().width()
    private fun currImageHeight() = getDisplayRectF().height()

    private var isDoubleTap = false
    private var preScrollX = 0f
    private var preScrollY = 0f
    private val scroller = Scroller(context)
    private val gListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            onScroll(-distanceX, -distanceY)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val startX = -currTranslateX().roundToInt()
            val startY = -currTranslateY().roundToInt()
            val velX = -velocityX.roundToInt()
            val velY = -velocityY.roundToInt()
            val maxX = (currImageWidth() - width).roundToInt()
            val maxY = (currImageHeight() - height).roundToInt()
            scroller.fling(startX, startY, velX, velY, 0, maxX, 0, maxY)
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            e?.let { event ->
                if (isDoubleTap) {
                    onScale(1 / 2f, event.x, event.y)
                } else {
                    onScale(2f, event.x, event.y)
                }
                isDoubleTap = !isDoubleTap
            }
            return true
        }
    }

    private val sgListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onScale(detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }

    }

    private val gestureDetector = GestureDetector(context, gListener)
    private val scaleGestureDetector = ScaleGestureDetector(context, sgListener)

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        update()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        update()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        update()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        update()
    }

    private fun update() {
        post {
            drawable?.apply {
                imageRectF.set(0f, 0f, intrinsicWidth.toFloat(), intrinsicHeight.toFloat())
            }
            checkMatrixBounds()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            scroller.forceFinished(true)
            val disallowIntercept = currImageWidth() > width || currImageHeight() > height
            parent.requestDisallowInterceptTouchEvent(disallowIntercept)
        }
        if (scaleGestureDetector.onTouchEvent(event)) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.isFinished) {
            return
        }
        if (scroller.computeScrollOffset()) {
            onScroll(preScrollX - scroller.currX, preScrollY - scroller.currY)
            preScrollX = scroller.currX.toFloat()
            preScrollY = scroller.currY.toFloat()
        }
    }

    private fun onScroll(dx: Float, dy: Float) {
        if (!scaleGestureDetector.isInProgress) {
            imageMatrix.postTranslate(dx, dy)
            checkMatrixBounds()
        }
    }

    private fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
        val sx = if (currScaleX() * scaleFactor > 1f) scaleFactor else 1 / currScaleX()
        val sy = if (currScaleY() * scaleFactor > 1f) scaleFactor else 1 / currScaleY()
        imageMatrix.postScale(sx, sy, focusX, focusY)
        checkMatrixBounds()
    }

    private fun checkMatrixBounds() {
        var dx = 0f
        var dy = 0f
        if (currImageWidth() < width) {
            dx = (width - currImageWidth()) * 0.5f - currTranslateX()
        } else {
            if (currTranslateX() > 0) {
                dx = -currTranslateX()
            }
            if (currTranslateX() + currImageWidth() < width) {
                dx = width - currImageWidth() - currTranslateX()
            }
        }
        if (currImageHeight() < height) {
            dy = (height - currImageHeight()) * 0.5f - currTranslateY()
        } else {
            if (currTranslateY() > 0) {
                dy = -currTranslateY()
            }
            if (currTranslateY() + currImageHeight() < height) {
                dy = height - currImageHeight() - currTranslateY()
            }
        }
        imageMatrix.postTranslate(dx, dy)
        postInvalidate()
    }

    fun canScrollDown(): Boolean {
        return if (currImageHeight() < height) {
            false
        } else {
            currTranslateY() != 0f
        }
    }

}
