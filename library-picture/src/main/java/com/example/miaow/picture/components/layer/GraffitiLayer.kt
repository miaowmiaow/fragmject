package com.example.miaow.picture.components.layer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import com.example.miaow.picture.components.PictureEditorState
import com.example.miaow.picture.data.PaintPath
import java.util.Stack
import kotlin.math.abs

class GraffitiLayer(private val state: PictureEditorState) : ILayer {

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
        paint.isAntiAlias = true
        paint.isDither = true
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = DEFAULT_PAINT_SIZE
    }

    fun setParentScale(scale: Float) {
        paint.strokeWidth = DEFAULT_PAINT_SIZE / scale
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
            state.invalidate()
        }
        return !paintPaths.empty()
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
                        val x = (event.x + touchX) * 0.5f
                        val y = (event.y + touchY) * 0.5f
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
            graffitiCanvas.drawPath(path, paint)
            state.invalidate()
        }
        return isEnabled
    }

    override fun onSizeChanged(
        viewWidth: Int,
        viewHeight: Int,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        graffitiBitmap = createBitmap(bitmapWidth, bitmapHeight)
        graffitiCanvas.setBitmap(graffitiBitmap)
        if (paintPaths.isNotEmpty()) {
            graffitiCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
            for (linePath in paintPaths) {
                graffitiCanvas.drawPath(linePath.path, linePath.paint)
            }
            state.invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(graffitiBitmap, 0f, 0f, null)
    }
}