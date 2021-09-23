package com.example.fragment.library.base.picture.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Scroller
import androidx.annotation.ColorInt
import com.example.fragment.library.base.R
import com.example.fragment.library.base.picture.editor.bean.StickerAttrs
import com.example.fragment.library.base.picture.editor.layer.GraffitiLayer
import com.example.fragment.library.base.picture.editor.layer.MosaicLayer
import com.example.fragment.library.base.picture.editor.layer.OnStickerClickListener
import com.example.fragment.library.base.picture.editor.layer.StickerLayer
import java.util.*
import kotlin.math.abs

class PictureEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val INVALID_ID = -1
        private const val MOSAIC_COEFFICIENT = 36
        private const val BIN_AREA_WIDTH = 300
        private const val BIN_AREA_HEIGHT = 200
        private const val BIN_AREA_ROUND = 30
        private const val BIN_ICON_WIDTH = 70
        private const val BIN_TEXT_SIZE = 30f
        private const val BIN_TEXT = "拖动到此处删除"
    }

    enum class Mode {
        GRAFFITI, ERASER, MOSAIC, STICKER
    }

    private val picEditorMatrix = Matrix()
    private val picEditorMatrixValues = FloatArray(9)
    private var currScaleX = 1f
    private var currScaleY = 1f
    private var currOffsetX = 0f
    private var currOffsetY = 0f
    private var currScrollX = 0f
    private var currScrollY = 0f
    private var initTranslateX = 0f
    private var initTranslateY = 0f

    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private val bitmapRectF = RectF()
    private val bitmapOptions = BitmapFactory.Options()
    private var mosaicBitmap: Bitmap? = null

    private val mosaicLayer = MosaicLayer(this)
    private val graffitiLayer = GraffitiLayer(this)
    private val stickerLayers = Stack<StickerLayer>()
    private var stickerLayerIndex = INVALID_ID
    private var pointerIndexId0 = INVALID_ID

    private val binIcon = BitmapFactory.decodeResource(resources, R.drawable.pe_bin)
    private val binAreaPaint = Paint()
    private val binTextPaint = Paint()
    private val binAreaRectF = RectF()
    private val binIconRectF = RectF()
    private var binTextWidth = 0f
    private var binTextSize = BIN_TEXT_SIZE
    private var binTextBaselineY = 0f
    private var binTextX = 0f
    private var binTextY = 0f
    private var isBin = false
    private var bitmapPath = ""

    private val scroller = Scroller(context)
    private val gestureListener = object : GestureDetector.OnGestureListener {

        override fun onDown(e: MotionEvent?): Boolean {
            if (!scroller.isFinished) {
                scroller.forceFinished(true)
            }
            return false
        }

        override fun onShowPress(e: MotionEvent?) {}

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return false
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            updateMatrixValues()
            onScroll(-distanceX, -distanceY)
            return true
        }

        override fun onLongPress(e: MotionEvent?) {}

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            updateMatrixValues()
            scroller.fling(
                (-currOffsetX).toInt(),
                (-currOffsetY).toInt(),
                (-velocityX).toInt(),
                (-velocityY).toInt(),
                0,
                (bitmapWidth * currScaleX - viewWidth).toInt(),
                0,
                (bitmapHeight * currScaleY - viewHeight).toInt()
            )
            return true
        }
    }

    private val scaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                updateMatrixValues()
                val scaleFactor = detector.scaleFactor
                val focusX = detector.focusX
                val focusY = detector.focusY
                val maxOffsetX: Float
                val maxOffsetY: Float
                if (currOffsetX > initTranslateX) {
                    picEditorMatrix.postTranslate(initTranslateX - currOffsetX, 0f)
                }
                if (currOffsetY > initTranslateY) {
                    picEditorMatrix.postTranslate(0f, initTranslateY - currOffsetY)
                }
                if (currScaleX * scaleFactor > 1f && currScaleY * scaleFactor > 1f) {
                    picEditorMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                    maxOffsetX = currOffsetX + bitmapWidth * currScaleX * scaleFactor
                    maxOffsetY = currOffsetY + bitmapHeight * currScaleY * scaleFactor
                } else {
                    picEditorMatrix.postScale(1 / currScaleX, 1 / currScaleY, focusX, focusY)
                    maxOffsetX = currOffsetX + bitmapWidth
                    maxOffsetY = currOffsetY + bitmapHeight
                }
                if (maxOffsetX < viewWidth - initTranslateX) {
                    picEditorMatrix.postTranslate(viewWidth - initTranslateX - maxOffsetX, 0f)
                }
                if (maxOffsetY < viewHeight - initTranslateY) {
                    picEditorMatrix.postTranslate(0f, viewHeight - initTranslateY - maxOffsetY)
                }
                invalidate()
                return true
            }

        }

    private val gestureDetector = GestureDetector(context, gestureListener)
    private val scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)

    init {
        binAreaPaint.style = Paint.Style.FILL
        binTextPaint.color = Color.WHITE
        binTextPaint.textSize = binTextSize
        binTextPaint.style = Paint.Style.STROKE
        binTextWidth = binTextPaint.measureText(BIN_TEXT)
        binTextBaselineY = abs(binTextPaint.ascent() + binTextPaint.descent()) / 2
    }

    fun setBitmapPath(path: String) {
        this.bitmapPath = path
        invalidate()
    }

    fun setMode(mode: Mode) {
        graffitiLayer.isEnabled = false
        mosaicLayer.isEnabled = false
        stickerLayers.forEach { sticker ->
            sticker.isEnabled = false
        }
        if (mode == Mode.MOSAIC) {
            mosaicLayer.isEnabled = true
        } else if (mode == Mode.GRAFFITI || mode == Mode.ERASER) {
            graffitiLayer.isEnabled = true
            graffitiLayer.setPaintMode(mode)
        } else if (mode == Mode.STICKER) {
            stickerLayers.forEach { layer ->
                layer.isEnabled = true
            }
        }
    }

    fun setGraffitiColor(@ColorInt color: Int) {
        graffitiLayer.setPaintColor(color)
    }

    fun graffitiUndo() {
        graffitiLayer.undo()
    }

    fun mosaicUndo() {
        mosaicLayer.undo()
    }

    fun setTextSticker(attrs: StickerAttrs, listener: OnStickerClickListener) {
        val stickerLayer = StickerLayer(this, attrs, object : OnStickerClickListener {
            override fun onClick(attrs: StickerAttrs) {
                if (stickerLayerIndex != INVALID_ID) {
                    stickerLayers.remove(stickerLayers[stickerLayerIndex])
                    stickerLayerIndex = INVALID_ID
                }
                listener.onClick(attrs)
            }
        })
        stickerLayer.onSizeChanged(viewWidth, viewHeight)
        stickerLayers.push(stickerLayer)
    }

    fun saveBitmap(): Bitmap {
        picEditorMatrix.reset()
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        draw(canvas)
        return bitmap
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        updateMatrixValues()
        val pointerId0 = event.getPointerId(0)
        pointerIndexId0 = event.findPointerIndex(pointerId0)
        // 计算缩放后的坐标
        val layerEvent = MotionEvent.obtain(event)
        val layerX = (event.x - currOffsetX) / currScaleX
        val layerY = (event.y - currOffsetY) / currScaleY
        layerEvent.setLocation(layerX, layerY)
        if (stickerLayerIndex == INVALID_ID) {
            for (index in 0 until stickerLayers.size) {
                if (stickerLayers[index].inTextBounds(layerEvent.x, layerEvent.y)) {
                    stickerLayerIndex = index
                    break
                }
            }
        }
        if (stickerLayerIndex != INVALID_ID) {
            if (stickerLayers[stickerLayerIndex].onTouchEvent(layerEvent)) {
                isBin = true
                if (binAreaRectF.contains(layerX, layerY)) {
                    binAreaPaint.setARGB(127, 255, 0, 0)
                } else {
                    binAreaPaint.setARGB(127, 0, 0, 0)
                }
                layerEvent.recycle()
                return true
            }
        }
        if (mosaicLayer.onTouchEvent(layerEvent)) {
            layerEvent.recycle()
            return true
        }
        if (graffitiLayer.onTouchEvent(layerEvent)) {
            graffitiLayer.setPaintStrokeWidthScale(currScaleX)
            layerEvent.recycle()
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            isBin = false
            if (stickerLayerIndex != INVALID_ID && event.getPointerId(0) == pointerIndexId0) {
                if (binAreaRectF.contains(layerEvent.x, layerEvent.y)) {
                    stickerLayers.remove(stickerLayers[stickerLayerIndex])
                }
            }
            stickerLayerIndex = INVALID_ID
        }
        layerEvent.recycle()
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        if (bitmapPath.isNotBlank()) {
            bitmapOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(bitmapPath, bitmapOptions)
            bitmapWidth = bitmapOptions.outWidth
            bitmapHeight = bitmapOptions.outHeight
            bitmapOptions.inJustDecodeBounds = false
            bitmapOptions.inScaled = true
            bitmapOptions.inDensity = bitmapWidth
            bitmapOptions.inTargetDensity = viewWidth
            val bitmap = BitmapFactory.decodeFile(bitmapPath, bitmapOptions)
            bitmapWidth = bitmap.width
            bitmapHeight = bitmap.height
            bitmapRectF.set(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
            picEditorMatrix.reset()
            if (bitmapWidth <= viewWidth) {
                initTranslateX = (viewWidth - bitmapWidth) * 0.5f
                if (bitmapHeight < viewHeight) {
                    initTranslateY = (viewHeight - bitmapHeight) * 0.5f
                }
                picEditorMatrix.postTranslate(initTranslateX, initTranslateY)
            }
            val mosaicWidth = bitmapWidth / MOSAIC_COEFFICIENT
            val mosaicHeight = bitmapHeight / MOSAIC_COEFFICIENT
            mosaicBitmap = Bitmap.createScaledBitmap(bitmap, mosaicWidth, mosaicHeight, false)
            mosaicLayer.setParentBitmap(bitmap)
            mosaicLayer.onSizeChanged(bitmapWidth, bitmapHeight)
            graffitiLayer.onSizeChanged(bitmapWidth, bitmapHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mosaicBitmap?.let {
            canvas.setMatrix(picEditorMatrix)
            canvas.drawBitmap(it, null, bitmapRectF, null)
        }
        mosaicLayer.onDraw(canvas)
        graffitiLayer.onDraw(canvas)
        stickerLayers.forEach { layer ->
            layer.onDraw(canvas)
        }
        if (isBin) {
            canvas.drawRoundRect(
                binAreaRectF,
                BIN_AREA_ROUND / currScaleX,
                BIN_AREA_ROUND / currScaleX,
                binAreaPaint
            )
            canvas.drawBitmap(binIcon, null, binIconRectF, null)
            binTextPaint.textSize = binTextSize
            canvas.drawText(BIN_TEXT, binTextX, binTextY, binTextPaint)
        }
    }

    override fun computeScroll() {
        super.computeScroll()
        if (!scroller.isFinished && scroller.computeScrollOffset()) {
            updateMatrixValues()
            onScroll(currScrollX - scroller.currX, currScrollY - scroller.currY)
            currScrollX = scroller.currX.toFloat()
            currScrollY = scroller.currY.toFloat()
        }
    }

    private fun onScroll(dx: Float, dy: Float) {
        if (currOffsetX + dx <= 0 && currOffsetX + dx >= -(bitmapWidth * currScaleX - viewWidth)) {
            picEditorMatrix.postTranslate(dx, 0f)
        }
        if (currOffsetY + dy <= 0 && currOffsetY + dy >= -(bitmapHeight * currScaleY - viewHeight)) {
            picEditorMatrix.postTranslate(0f, dy)
        }
        invalidate()
    }

    private fun updateMatrixValues() {
        picEditorMatrix.getValues(picEditorMatrixValues)
        currScaleX = picEditorMatrixValues[0]
        currScaleY = picEditorMatrixValues[4]
        currOffsetX = picEditorMatrixValues[2]
        currOffsetY = picEditorMatrixValues[5]
        val binLeft = ((viewWidth - BIN_AREA_WIDTH) * 0.5f - currOffsetX) / currScaleX
        val binTop = (viewHeight - BIN_AREA_HEIGHT - BIN_AREA_ROUND - currOffsetY) / currScaleY
        val binRight = binLeft + BIN_AREA_WIDTH / currScaleX
        val binBottom = binTop + BIN_AREA_HEIGHT / currScaleY
        binAreaRectF.set(binLeft, binTop, binRight, binBottom)
        binIconRectF.set(
            binAreaRectF.centerX() - ((BIN_ICON_WIDTH / 2) / currScaleX),
            binAreaRectF.top + BIN_AREA_ROUND / currScaleX,
            binAreaRectF.centerX() + ((BIN_ICON_WIDTH / 2) / currScaleX),
            binAreaRectF.top + (BIN_AREA_ROUND + BIN_ICON_WIDTH) / currScaleY
        )
        binTextX = binAreaRectF.centerX() - ((binTextWidth / 2) / currScaleX)
        binTextY = binAreaRectF.bottom - BIN_AREA_ROUND / currScaleY
        binTextSize = BIN_TEXT_SIZE / currScaleX
    }

}
