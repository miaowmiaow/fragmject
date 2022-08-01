package com.example.miaow.picture.editor.view.layer

import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.View.LAYER_TYPE_HARDWARE
import androidx.annotation.ColorInt
import com.example.miaow.picture.editor.bean.PaintPath
import com.example.miaow.picture.editor.view.PictureEditorView
import java.util.*
import kotlin.math.abs

class GraffitiLayer(private val parent: View) : ILayer {

    companion object {
        private const val DEFAULT_PAINT_SIZE = 25.0f
        private const val DEFAULT_ERASER_SIZE = 50.0f
        private const val TOUCH_TOLERANCE = 4f
    }

    private lateinit var graffitiBitmap: Bitmap
    private var graffitiCanvas = Canvas()
    private val paintPaths = Stack<PaintPath>()
    private val redoPaths = Stack<PaintPath>()
    private val paint = Paint()
    private val path = Path()
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
        setPaintMode(PictureEditorView.Mode.GRAFFITI)
    }

    fun setParentScale(scale: Float) {
        paint.strokeWidth = DEFAULT_PAINT_SIZE / scale
    }

    fun setPaintMode(mode: PictureEditorView.Mode) {
        if (mode == PictureEditorView.Mode.GRAFFITI) {
            paint.strokeWidth = DEFAULT_PAINT_SIZE
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        } else if (mode == PictureEditorView.Mode.ERASER) {
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
            graffitiCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
            redoPaths.push(paintPaths.pop())
            for (linePath in paintPaths) {
                graffitiCanvas.drawPath(linePath.path, linePath.paint)
            }
            parent.postInvalidate()
        }
        return !paintPaths.empty()
    }

    fun redo(): Boolean {
        if (redoPaths.isNotEmpty()) {
            path.reset()
            graffitiCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
            paintPaths.push(redoPaths.pop())
            for (linePath in paintPaths) {
                graffitiCanvas.drawPath(linePath.path, linePath.paint)
            }
            parent.postInvalidate()
        }
        return !redoPaths.empty()
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
            graffitiCanvas.drawPath(path, paint)
            parent.postInvalidate()
        }
        return isEnabled
    }

    override fun onSizeChanged(
        viewWidth: Int,
        viewHeight: Int,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        graffitiBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        graffitiCanvas.setBitmap(graffitiBitmap)
        if (paintPaths.isNotEmpty()) {
            graffitiCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
            for (linePath in paintPaths) {
                graffitiCanvas.drawPath(linePath.path, linePath.paint)
            }
            parent.postInvalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(graffitiBitmap, 0f, 0f, null)
    }

}

