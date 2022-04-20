package com.example.miaow.picture.editor.layer

import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.example.miaow.picture.editor.bean.PaintPath
import java.util.*
import kotlin.math.abs

class MosaicLayer(private val parent: View) : ILayer {

    companion object {
        private const val DEFAULT_PAINT_SIZE = 30.0f
        private const val TOUCH_TOLERANCE = 4f
    }

    private lateinit var mosaicBitmap: Bitmap
    private var mosaicCanvas = Canvas()
    private var parentBitmap: Bitmap? = null
    private val paintPaths = Stack<PaintPath>()
    private val redoPaths = Stack<PaintPath>()
    private val rectF = RectF()
    private val paint = Paint()
    private val path = Path()
    private var touchX = 0f
    private var touchY = 0f

    var isEnabled = false

    init {
        paint.alpha = 0
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = DEFAULT_PAINT_SIZE
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    fun setParentBitmap(bitmap: Bitmap) {
        parentBitmap = bitmap
    }

    fun setParentScale(scale: Float) {
        paint.strokeWidth = DEFAULT_PAINT_SIZE / scale
    }

    fun undo(): Boolean {
        if (paintPaths.isNotEmpty()) {
            path.reset()
            parentBitmap?.let {
                mosaicCanvas.drawBitmap(it, null, rectF, null)
            }
            redoPaths.push(paintPaths.pop())
            for (linePath in paintPaths) {
                mosaicCanvas.drawPath(linePath.path, linePath.paint)
            }
            parent.invalidate()
        }
        return !paintPaths.empty()
    }

    fun redo(): Boolean {
        if (redoPaths.isNotEmpty()) {
            path.reset()
            parentBitmap?.let {
                mosaicCanvas.drawBitmap(it, null, rectF, null)
            }
            paintPaths.push(redoPaths.pop())
            for (linePath in paintPaths) {
                mosaicCanvas.drawPath(linePath.path, linePath.paint)
            }
            parent.invalidate()
        }
        return !redoPaths.empty()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    path.reset()
                    path.moveTo(event.x, event.y)
                    touchX = event.x
                    touchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.x - touchX)
                    val dy = abs(event.y - touchY)
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        val x = (event.x + touchX) / 2
                        val y = (event.y + touchY) / 2
                        path.quadTo(touchX, touchY, x, y)
                        touchX = event.x
                        touchY = event.y
                    }
                }
                MotionEvent.ACTION_UP -> {
                    path.lineTo(event.x, event.y)
                    paintPaths.push(PaintPath(path, paint))
                }
            }
            mosaicCanvas.drawPath(path, paint)
            parent.invalidate()
        }
        return isEnabled
    }

    override fun onSizeChanged(
        viewWidth: Int,
        viewHeight: Int,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        rectF.set(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
        mosaicBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        mosaicCanvas.setBitmap(mosaicBitmap)
        parentBitmap?.let {
            mosaicCanvas.drawBitmap(it, null, rectF, null)
        }
        if (paintPaths.isNotEmpty()) {
            mosaicCanvas.drawBitmap(mosaicBitmap, null, rectF, null)
            for (linePath in paintPaths) {
                mosaicCanvas.drawPath(linePath.path, linePath.paint)
            }
            parent.invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(mosaicBitmap, 0f, 0f, null)
    }

}
