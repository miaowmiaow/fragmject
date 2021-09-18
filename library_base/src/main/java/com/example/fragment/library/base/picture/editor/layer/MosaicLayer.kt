package com.example.fragment.library.base.picture.editor.layer

import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.example.fragment.library.base.picture.editor.bean.PaintPath
import java.util.*
import kotlin.math.abs

class MosaicLayer(private val parentView: View) : ILayer {

    companion object {
        private const val DEFAULT_PAINT_SIZE = 50.0f
        private const val TOUCH_TOLERANCE = 4f
    }

    private val mosaicPaint = Paint()
    private val mosaicPath = Path()
    private val paintSize = DEFAULT_PAINT_SIZE

    private val mosaicPaintPaths = Stack<PaintPath>()
    private val redoPaintPaths = Stack<PaintPath>()

    private var _parentBitmap: Bitmap? = null
    private val parentBitmap get() = _parentBitmap!!
    private var _mosaicBitmap: Bitmap? = null
    private val mosaicBitmap get() = _mosaicBitmap!!
    private var _mosaicCanvas: Canvas? = null
    private val mosaicCanvas get() = _mosaicCanvas!!

    private val mosaicRectF = RectF()

    private var touchX = 0f
    private var touchY = 0f

    var isEnabled = false

    init {
        mosaicPaint.alpha = 0
        mosaicPaint.style = Paint.Style.STROKE
        mosaicPaint.strokeJoin = Paint.Join.ROUND
        mosaicPaint.strokeCap = Paint.Cap.ROUND
        mosaicPaint.strokeWidth = paintSize
        mosaicPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    fun undo(): Boolean {
        if (mosaicPaintPaths.isNotEmpty()) {
            mosaicPath.reset()
            mosaicCanvas.drawBitmap(parentBitmap, null, mosaicRectF, null)
            redoPaintPaths.push(mosaicPaintPaths.pop())
            for (linePath in mosaicPaintPaths) {
                mosaicCanvas.drawPath(linePath.path, linePath.paint)
            }
            parentView.invalidate()
        }
        return !mosaicPaintPaths.empty()
    }

    fun redo(): Boolean {
        if (redoPaintPaths.isNotEmpty()) {
            mosaicPath.reset()
            mosaicCanvas.drawBitmap(parentBitmap, null, mosaicRectF, null)
            mosaicPaintPaths.push(redoPaintPaths.pop())
            for (linePath in mosaicPaintPaths) {
                mosaicCanvas.drawPath(linePath.path, linePath.paint)
            }
            parentView.invalidate()
        }
        return !redoPaintPaths.empty()
    }

    fun setParentBitmap(bitmap: Bitmap) {
        _parentBitmap = bitmap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            val touchX = event.x
            val touchY = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mosaicPath.reset()
                    mosaicPath.moveTo(touchX, touchY)
                    this.touchX = touchX
                    this.touchY = touchY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(touchX - this.touchX)
                    val dy = abs(touchY - this.touchY)
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        mosaicPath.quadTo(
                            this.touchX,
                            this.touchY,
                            (touchX + this.touchX) / 2,
                            (touchY + this.touchY) / 2
                        )
                        this.touchX = touchX
                        this.touchY = touchY
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mosaicPath.lineTo(touchX, touchY)
                    mosaicPaintPaths.push(PaintPath(mosaicPath, mosaicPaint))
                }
            }
            parentView.invalidate()
        }
        return isEnabled
    }

    override fun onSizeChanged(w: Int, h: Int) {
        mosaicRectF.set(0f, 0f, w.toFloat(), h.toFloat())
        _mosaicBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        _mosaicCanvas = Canvas(mosaicBitmap)
        mosaicCanvas.drawBitmap(parentBitmap, null, mosaicRectF, null)
        if (mosaicPaintPaths.isNotEmpty()) {
            mosaicCanvas.drawBitmap(parentBitmap, null, mosaicRectF, null)
            for (linePath in mosaicPaintPaths) {
                mosaicCanvas.drawPath(linePath.path, linePath.paint)
            }
            parentView.invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        mosaicCanvas.drawPath(mosaicPath, mosaicPaint)
        canvas.drawBitmap(mosaicBitmap, 0f, 0f, null)
    }

}
