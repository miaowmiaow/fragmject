package com.example.fragment.library.base.picture.editor.layer

import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.View.LAYER_TYPE_HARDWARE
import androidx.annotation.ColorInt
import com.example.fragment.library.base.picture.editor.bean.EditorMode
import com.example.fragment.library.base.picture.editor.bean.PaintPath
import java.util.*
import kotlin.math.abs

class GraffitiLayer(private val parentView: View) : ILayer {

    companion object {
        private const val DEFAULT_PAINT_SIZE = 25.0f
        private const val DEFAULT_ERASER_SIZE = 50.0f
        private const val TOUCH_TOLERANCE = 4f
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private val graffitiPaint = Paint()
    private val graffitiPath = Path()
    private var paintSize = DEFAULT_PAINT_SIZE
    private val eraserSize = DEFAULT_ERASER_SIZE

    private var _graffitiBitmap: Bitmap? = null
    private val graffitiBitmap get() = _graffitiBitmap!!
    private var _graffitiCanvas: Canvas? = null
    private val graffitiCanvas get() = _graffitiCanvas!!

    private val graffitiPaintPaths = Stack<PaintPath>()
    private val redoPaintPaths = Stack<PaintPath>()

    private var touchX = 0f
    private var touchY = 0f

    var isEnabled = false

    init {
        //禁用硬件加速，使橡皮擦功能正常工作
        parentView.setLayerType(LAYER_TYPE_HARDWARE, null)
        graffitiPaint.isAntiAlias = true
        graffitiPaint.isDither = true
        graffitiPaint.strokeCap = Paint.Cap.ROUND
        graffitiPaint.strokeJoin = Paint.Join.ROUND
        graffitiPaint.style = Paint.Style.STROKE
        setPaintMode(EditorMode.GRAFFITI)
    }

    fun setPaintStrokeWidthScale(scale: Float) {
        graffitiPaint.strokeWidth = paintSize / scale
    }

    fun setPaintMode(editorMode: EditorMode) {
        if (editorMode == EditorMode.GRAFFITI) {
            graffitiPaint.strokeWidth = paintSize
            graffitiPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        } else if (editorMode == EditorMode.ERASER) {
            graffitiPaint.strokeWidth = eraserSize
            graffitiPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    fun setPaintColor(@ColorInt color: Int) {
        graffitiPaint.color = color
    }

    fun undo(): Boolean {
        if (graffitiPaintPaths.isNotEmpty()) {
            graffitiPath.reset()
            graffitiCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
            redoPaintPaths.push(graffitiPaintPaths.pop())
            for (linePath in graffitiPaintPaths) {
                graffitiCanvas.drawPath(linePath.path, linePath.paint)
            }
            parentView.invalidate()
        }
        return !graffitiPaintPaths.empty()
    }

    fun redo(): Boolean {
        if (redoPaintPaths.isNotEmpty()) {
            graffitiPath.reset()
            graffitiCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
            graffitiPaintPaths.push(redoPaintPaths.pop())
            for (linePath in graffitiPaintPaths) {
                graffitiCanvas.drawPath(linePath.path, linePath.paint)
            }
            parentView.invalidate()
        }
        return !redoPaintPaths.empty()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            val touchX = event.x
            val touchY = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    graffitiPath.reset()
                    graffitiPath.moveTo(touchX, touchY)
                    this.touchX = touchX
                    this.touchY = touchY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(touchX - this.touchX)
                    val dy = abs(touchY - this.touchY)
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        graffitiPath.quadTo(
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
                    graffitiPath.lineTo(touchX, touchY)
                    graffitiPaintPaths.push(PaintPath(graffitiPath, graffitiPaint))
                }
            }
            graffitiCanvas.drawPath(graffitiPath, graffitiPaint)
            parentView.invalidate()
        }
        return isEnabled
    }

    override fun onSizeChanged(w: Int, h: Int) {
        this.viewWidth = w
        this.viewHeight = h
        _graffitiBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        _graffitiCanvas = Canvas(graffitiBitmap)
        if (graffitiPaintPaths.isNotEmpty()) {
            graffitiCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
            for (linePath in graffitiPaintPaths) {
                graffitiCanvas.drawPath(linePath.path, linePath.paint)
            }
            parentView.invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(graffitiBitmap, 0f, 0f, null)
    }

}

