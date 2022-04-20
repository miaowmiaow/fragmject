package com.example.miaow.picture.selector.view

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.Drawable.createFromPath
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.annotation.RequiresApi
import androidx.core.graphics.values
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

class PicturePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val MAX_BITMAP_SIZE = 64f * 1024 * 1024
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var preScrollX = 0f
    private var preScrollY = 0f
    private val bitmapOptions = BitmapFactory.Options()
    private val bitmapMatrix = Matrix()
    private val bitmapRectF = RectF()
    private var bitmap: Bitmap? = null
    private var isDoubleTap = false
    private var bitmapPath = ""

    private val scroller = OverScroller(context)
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
            val startX = (-currTranslateX()).roundToInt()
            val startY = (-currTranslateY()).roundToInt()
            val velX = (-velocityX).roundToInt()
            val velY = (-velocityY).roundToInt()
            val maxX = (bitmapRectF.width() * currScaleX() - viewWidth).roundToInt()
            val maxY = (bitmapRectF.height() * currScaleY() - viewHeight).roundToInt()
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

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            resetScaleOffset()
        }

    }

    private val gestureDetector = GestureDetector(context, gListener)
    private val scaleGestureDetector = ScaleGestureDetector(context, sgListener)

    fun setBitmapPath(path: String) {
        this.bitmapPath = path
        postInvalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            scroller.forceFinished(true)
        }
        if (gestureDetector.onTouchEvent(event)) {
            scaleGestureDetector.onTouchEvent(event)
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.viewWidth = max(w, oldw)
        this.viewHeight = max(h, oldh)
        if (bitmapPath.isNotBlank()) {
            bitmapOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(bitmapPath, bitmapOptions)
            bitmapWidth = bitmapOptions.outWidth
            bitmapHeight = bitmapOptions.outHeight
            bitmapOptions.inJustDecodeBounds = false
            bitmapOptions.inScaled = true
            bitmapOptions.inDensity = bitmapWidth
            bitmapOptions.inTargetDensity = viewWidth
            bitmap = BitmapFactory.decodeFile(bitmapPath, bitmapOptions)?.apply {
                bitmapWidth = width
                bitmapHeight = height
                if (byteCount > MAX_BITMAP_SIZE) {
                    val bitmapDensity = MAX_BITMAP_SIZE / byteCount
                    bitmapWidth = (bitmapWidth * bitmapDensity).toInt()
                    bitmapHeight = (bitmapHeight * bitmapDensity).toInt()
                }
                bitmapRectF.set(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
                if (bitmapWidth < viewWidth) {
                    val initTranslateX = (viewWidth - bitmapRectF.width() * currScaleX()) * 0.5f
                    val dx = initTranslateX - currTranslateX()
                    bitmapMatrix.postTranslate(dx, 0f)
                }
                if (bitmapHeight < viewHeight) {
                    val initTranslateY = (viewHeight - bitmapRectF.height() * currScaleY()) * 0.5f
                    val dy = initTranslateY - currTranslateY()
                    bitmapMatrix.postTranslate(0f, dy)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.setMatrix(bitmapMatrix)
        bitmap?.let {
            canvas.drawBitmap(it, null, bitmapRectF, null)
        }
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
        val currBitmapWidth = bitmapRectF.width() * currScaleX()
        val currBitmapHeight = bitmapRectF.height() * currScaleY()
        if (currTranslateX() + dx <= 0 && currTranslateX() + dx >= viewWidth - currBitmapWidth) {
            bitmapMatrix.postTranslate(dx, 0f)
        }
        if (currTranslateY() + dy <= 0 && currTranslateY() + dy >= viewHeight - currBitmapHeight) {
            bitmapMatrix.postTranslate(0f, dy)
        }
        postInvalidate()
    }

    private fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
        val sx = if (currScaleX() * scaleFactor > 1f) scaleFactor else 1 / currScaleX()
        val sy = if (currScaleY() * scaleFactor > 1f) scaleFactor else 1 / currScaleY()
        bitmapMatrix.postScale(sx, sy, focusX, focusY)
        resetScaleOffset()
    }

    private fun resetScaleOffset() {
        var dx = 0f
        var dy = 0f
        val currBitmapWidth = bitmapRectF.width() * currScaleX()
        val currBitmapHeight = bitmapRectF.height() * currScaleY()
        if (bitmapWidth < viewWidth) {
            dx = (viewWidth - currBitmapWidth) * 0.5f - currTranslateX()
        } else {
            if (currTranslateX() > 0) {
                dx = -currTranslateX()
            }
            if (currTranslateX() + currBitmapWidth < viewWidth) {
                dx = viewWidth - currBitmapWidth - currTranslateX()
            }
        }
        if (bitmapHeight < viewHeight) {
            dy = (viewHeight - currBitmapHeight) * 0.5f - currTranslateY()
        } else {
            if (currTranslateY() > 0) {
                dy = -currTranslateY()
            }
            if (currTranslateY() + currBitmapHeight < viewHeight) {
                dy = viewHeight - currBitmapHeight - currTranslateY()
            }
        }
        bitmapMatrix.postTranslate(dx, dy)
        postInvalidate()
    }

    private fun currScaleX(): Float {
        return bitmapMatrix.values()[0]
    }

    private fun currScaleY(): Float {
        return bitmapMatrix.values()[4]
    }

    private fun currTranslateX(): Float {
        return bitmapMatrix.values()[2]
    }

    private fun currTranslateY(): Float {
        return bitmapMatrix.values()[5]
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getDrawableFromUri(uri: Uri): Bitmap? {
        val scheme = uri.scheme
        if (ContentResolver.SCHEME_CONTENT == scheme || ContentResolver.SCHEME_FILE == scheme) {
            try {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

}
