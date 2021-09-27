package com.example.miaow.picture.editor.layer

import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.View.LAYER_TYPE_HARDWARE
import androidx.annotation.ColorInt
import com.example.miaow.picture.bean.PaintPath
import java.util.*
import kotlin.math.abs

class GraffitiLayer(val parent: View) : ILayer {

    companion object {
        private const val DEFAULT_PAINT_SIZE = 25.0f
        private const val DEFAULT_ERASER_SIZE = 50.0f
        private const val TOUCH_TOLERANCE = 4f
    }

    private var _bitmap: Bitmap? = null
    private val bitmap get() = _bitmap!!
    private var _canvas: Canvas? = null
    private val canvas get() = _canvas!!

    private val paint = Paint()
    private val path = Path()

    private val paintPaths = Stack<PaintPath>()
    private val redoPaintPaths = Stack<PaintPath>()

    private var touchX = 0f
    private var touchY = 0f

    var isEnabled = false

    init {
        //禁用硬件加速，使橡皮擦功能正常工作
        parent.setLayerType(LAYER_TYPE_HARDWARE, null)
        paint.isAntiAlias = true
        paint.isDither = true
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.style = Paint.Style.STROKE
        setPaintMode(com.example.miaow.picture.editor.PictureEditorView.Mode.GRAFFITI)
    }

    fun setPaintStrokeWidthScale(scale: Float) {
        paint.strokeWidth = DEFAULT_PAINT_SIZE / scale
    }

    fun setPaintMode(mode: com.example.miaow.picture.editor.PictureEditorView.Mode) {
        if (mode == com.example.miaow.picture.editor.PictureEditorView.Mode.GRAFFITI) {
            paint.strokeWidth = DEFAULT_PAINT_SIZE
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        } else if (mode == com.example.miaow.picture.editor.PictureEditorView.Mode.ERASER) {
            paint.strokeWidth = DEFAULT_ERASER_SIZE
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    fun setPaintColor(@ColorInt color: Int) {
        paint.color = color
    }

    fun undo(): Boolean {
        if (paintPaths.isNotEmpty()) {
            path.reset()
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            redoPaintPaths.push(paintPaths.pop())
            for (linePath in paintPaths) {
                canvas.drawPath(linePath.path, linePath.paint)
            }
            parent.invalidate()
        }
        return !paintPaths.empty()
    }

    fun redo(): Boolean {
        if (redoPaintPaths.isNotEmpty()) {
            path.reset()
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            paintPaths.push(redoPaintPaths.pop())
            for (linePath in paintPaths) {
                canvas.drawPath(linePath.path, linePath.paint)
            }
            parent.invalidate()
        }
        return !redoPaintPaths.empty()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    path.reset()
                    path.moveTo(event.x, event.y)
                    this.touchX = event.x
                    this.touchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.x - this.touchX)
                    val dy = abs(event.y - this.touchY)
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        val x = (event.x + this.touchX) / 2
                        val y = (event.y + this.touchY) / 2
                        path.quadTo(this.touchX, this.touchY, x, y)
                        this.touchX = event.x
                        this.touchY = event.y
                    }
                }
                MotionEvent.ACTION_UP -> {
                    path.lineTo(event.x, event.y)
                    paintPaths.push(PaintPath(path, paint))
                }
            }
            canvas.drawPath(path, paint)
            parent.invalidate()
        }
        return isEnabled
    }

    override fun onSizeChanged(w: Int, h: Int) {
        _bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        _canvas = Canvas(bitmap)
        if (paintPaths.isNotEmpty()) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            for (linePath in paintPaths) {
                canvas.drawPath(linePath.path, linePath.paint)
            }
            parent.invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }

}

