package com.example.miaow.picture.editor.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Scroller
import androidx.annotation.ColorInt
import androidx.core.graphics.values
import com.example.miaow.base.utils.getBitmapFromPath
import com.example.miaow.base.utils.getBitmapFromUri
import com.example.miaow.picture.R
import com.example.miaow.picture.editor.bean.StickerAttrs
import com.example.miaow.picture.editor.view.layer.GraffitiLayer
import com.example.miaow.picture.editor.view.layer.MosaicLayer
import com.example.miaow.picture.editor.view.layer.OnStickerClickListener
import com.example.miaow.picture.editor.view.layer.StickerLayer
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class PictureEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val INVALID_ID = -1
        private const val MOSAIC_COEFFICIENT = 36 //马赛克系数
        private const val BIN_WIDTH = 300
        private const val BIN_HEIGHT = 200
        private const val BIN_ROUND = 30
        private const val BIN_MARGIN = 150
        private const val BIN_ICON_WIDTH = 70
        private const val BIN_TEXT_SIZE = 30f
        private const val BIN_TEXT = "拖动到此处删除"
    }

    enum class Mode {
        GRAFFITI, ERASER, MOSAIC, STICKER
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var preScrollX = 0f
    private var preScrollY = 0f
    private val bitmapMatrix = Matrix()
    private val bitmapRectF = RectF()
    private var mosaicBitmap: Bitmap? = null
    private val mosaicLayer = MosaicLayer(this)
    private val graffitiLayer = GraffitiLayer(this)
    private val stickerLayers = Stack<StickerLayer>()
    private var stickerLayerIndex = INVALID_ID
    private var pointerIndexId0 = INVALID_ID
    private val binIcon = BitmapFactory.decodeResource(resources, R.drawable.pe_bin)
    private val binPaint = Paint()
    private val binRectF = RectF()
    private val binIconRectF = RectF()
    private val binTextPaint = Paint()
    private var binTextWidth = 0f
    private var binTextSize = BIN_TEXT_SIZE
    private var binTextBaselineY = 0f
    private var binTextX = 0f
    private var binTextY = 0f
    private var isBin = false
    private var isDoubleTap = false
    private var bitmapPath: String? = null
    private var bitmapUri: Uri? = null

    private var initScaleX = 1f
    private var initScaleY = 1f
    private fun currScaleX() = bitmapMatrix.values()[0]
    private fun currScaleY() = bitmapMatrix.values()[4]
    fun currTranslateX() = bitmapMatrix.values()[2]
    fun currTranslateY() = bitmapMatrix.values()[5]

    private val scroller = Scroller(context)
    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                if (!scroller.isFinished) {
                    scroller.forceFinished(true)
                }
                return false
            }

            override fun onShowPress(e: MotionEvent) {}

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return false
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                onScroll(-distanceX, -distanceY)
                return true
            }

            override fun onLongPress(e: MotionEvent) {}

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val startX = (-currTranslateX()).toInt()
                val startY = (-currTranslateY()).toInt()
                val velX = (-velocityX).toInt()
                val velY = (-velocityY).toInt()
                val maxX = (bitmapRectF.width() * currScaleX() - viewWidth).toInt()
                val maxY = (bitmapRectF.height() * currScaleY() - viewHeight).toInt()
                scroller.fling(startX, startY, velX, velY, 0, maxX, 0, maxY)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isDoubleTap) {
                    onScale(1 / initScaleY / currScaleX(), e.x, e.y)
                } else {
                    val currBitmapWidth = bitmapRectF.width() * currScaleX()
                    onScale(viewWidth / currBitmapWidth, e.x, e.y)
                }
                isDoubleTap = !isDoubleTap
                return true
            }
        })
    private val scaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onScale(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                super.onScaleEnd(detector)
                resetScaleOffset()
            }

        })

    init {
        binPaint.style = Paint.Style.FILL
        binTextPaint.color = Color.WHITE
        binTextPaint.textSize = binTextSize
        binTextPaint.style = Paint.Style.STROKE
        binTextWidth = binTextPaint.measureText(BIN_TEXT)
        binTextBaselineY = abs(binTextPaint.ascent() + binTextPaint.descent()) * 0.5f
    }

    fun setBitmapPathOrUri(path: String?, uri: Uri?) {
        this.bitmapPath = path
        this.bitmapUri = uri
        initBitmap()
    }

    fun setMode(mode: Mode) {
        graffitiLayer.isEnabled = false
        mosaicLayer.isEnabled = false
        stickerLayers.forEach { sticker ->
            sticker.isEnabled = false
        }
        when (mode) {
            Mode.GRAFFITI, Mode.ERASER -> {
                graffitiLayer.isEnabled = true
                graffitiLayer.setPaintMode(mode)
            }

            Mode.MOSAIC -> {
                mosaicLayer.isEnabled = true
            }

            Mode.STICKER -> {
                stickerLayers.forEach { layer ->
                    layer.isEnabled = true
                }
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

    fun setSticker(attrs: StickerAttrs, listener: OnStickerClickListener? = null) {
        val stickerLayer = StickerLayer(this, attrs, object : OnStickerClickListener {
            override fun onClick(attrs: StickerAttrs) {
                if (stickerLayerIndex != INVALID_ID) {
                    stickerLayers.remove(stickerLayers[stickerLayerIndex])
                    stickerLayerIndex = INVALID_ID
                }
                listener?.onClick(attrs)
            }
        })
        stickerLayer.setParentMatrix(bitmapMatrix)
        val bitmapWidth = bitmapRectF.width().toInt()
        val bitmapHeight = bitmapRectF.height().toInt()
        stickerLayer.onSizeChanged(viewWidth, viewHeight, bitmapWidth, bitmapHeight)
        stickerLayers.push(stickerLayer)
        postInvalidate()
    }

    fun saveBitmap(): Bitmap {
        val tempMatrix = Matrix(bitmapMatrix)
        bitmapMatrix.reset()
        val width = max(bitmapRectF.width().toInt(), 1)
        val height = max(bitmapRectF.height().toInt(), 1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        draw(canvas)
        bitmapMatrix.set(tempMatrix)
        return bitmap
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerId0 = event.getPointerId(0)
        pointerIndexId0 = event.findPointerIndex(pointerId0)
        // 计算缩放后的坐标
        val layerEvent = MotionEvent.obtain(event)
        val layerX = (event.x - currTranslateX()) / currScaleX()
        val layerY = (event.y - currTranslateY()) / currScaleY()
        layerEvent.setLocation(layerX, layerY)
        if (stickerLayerIndex == INVALID_ID) {
            for (index in 0 until stickerLayers.size) {
                if (stickerLayers[index].inStickerBounds(layerEvent.x, layerEvent.y)) {
                    stickerLayerIndex = index
                    break
                }
            }
        }
        if (stickerLayerIndex != INVALID_ID) {
            if (stickerLayers[stickerLayerIndex].onTouchEvent(layerEvent)) {
                isBin = true
                val r = if (binRectF.contains(layerX, layerY)) 255 else 0
                binPaint.setARGB(127, r, 0, 0)
                computeBinRectF()
                layerEvent.recycle()
                return true
            }
        }
        if (mosaicLayer.onTouchEvent(layerEvent)) {
            mosaicLayer.setParentScale(currScaleX())
            layerEvent.recycle()
            return true
        }
        if (graffitiLayer.onTouchEvent(layerEvent)) {
            graffitiLayer.setParentScale(currScaleX())
            layerEvent.recycle()
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            isBin = false
            if (stickerLayerIndex != INVALID_ID && pointerId0 == pointerIndexId0) {
                if (binRectF.contains(layerEvent.x, layerEvent.y)) {
                    stickerLayers.remove(stickerLayers[stickerLayerIndex])
                }
            }
            stickerLayerIndex = INVALID_ID
        }
        layerEvent.recycle()
        if (gestureDetector.onTouchEvent(event)) {
            scaleGestureDetector.onTouchEvent(event)
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.viewWidth = max(w, oldw)
        this.viewHeight = max(h, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.setMatrix(bitmapMatrix)
        mosaicBitmap?.let {
            canvas.drawBitmap(it, null, bitmapRectF, null)
            mosaicLayer.onDraw(canvas)
            graffitiLayer.onDraw(canvas)
            stickerLayers.forEach { sticker ->
                sticker.onDraw(canvas)
            }
        }
        if (isBin) {
            val round = BIN_ROUND / currScaleX()
            canvas.drawRoundRect(binRectF, round, round, binPaint)
            canvas.drawBitmap(binIcon, null, binIconRectF, null)
            binTextPaint.textSize = binTextSize
            canvas.drawText(BIN_TEXT, binTextX, binTextY, binTextPaint)
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
        val sx =
            if (currScaleX() * scaleFactor > initScaleX) scaleFactor else initScaleX / currScaleX()
        val sy =
            if (currScaleY() * scaleFactor > initScaleY) scaleFactor else initScaleY / currScaleY()
        bitmapMatrix.postScale(sx, sy, focusX, focusY)
        resetScaleOffset()
    }

    private fun resetScaleOffset() {
        val currBitmapWidth = bitmapRectF.width() * currScaleX()
        val dx = if (currBitmapWidth < viewWidth) {
            (viewWidth - currBitmapWidth) * 0.5f - currTranslateX()
        } else {
            when {
                currTranslateX() > 0 -> {
                    -currTranslateX()
                }

                currTranslateX() + currBitmapWidth < viewWidth -> {
                    viewWidth - currBitmapWidth - currTranslateX()
                }

                else -> 0f
            }
        }
        val currBitmapHeight = bitmapRectF.height() * currScaleY()
        val dy = if (currBitmapHeight < viewHeight) {
            (viewHeight - currBitmapHeight) * 0.5f - currTranslateY()
        } else {
            when {
                currTranslateY() > 0 -> {
                    -currTranslateY()
                }

                currTranslateY() + currBitmapHeight < viewHeight -> {
                    viewHeight - currBitmapHeight - currTranslateY()
                }

                else -> 0f
            }
        }
        bitmapMatrix.postTranslate(dx, dy)
        postInvalidate()
    }

    private fun computeBinRectF() {
        val binLeft = ((viewWidth - BIN_WIDTH) * 0.5f - currTranslateX()) / currScaleX()
        val binTop = (viewHeight - BIN_HEIGHT - BIN_MARGIN - currTranslateY()) / currScaleY()
        val binRight = binLeft + BIN_WIDTH / currScaleX()
        val binBottom = binTop + BIN_HEIGHT / currScaleY()
        binRectF.set(binLeft, binTop, binRight, binBottom)
        val binIconLeft = binRectF.centerX() - (BIN_ICON_WIDTH / currScaleX()) * 0.5f
        val binIconTop = binRectF.top + BIN_ROUND / currScaleX()
        val binIconRight = binRectF.centerX() + (BIN_ICON_WIDTH / currScaleX()) * 0.5f
        val binIconBottom = binRectF.top + (BIN_ROUND + BIN_ICON_WIDTH) / currScaleY()
        binIconRectF.set(binIconLeft, binIconTop, binIconRight, binIconBottom)
        binTextX = binRectF.centerX() - (binTextWidth / currScaleX()) * 0.5f
        binTextY = binRectF.bottom - BIN_ROUND / currScaleY()
        binTextSize = BIN_TEXT_SIZE / currScaleX()
    }

    private fun initBitmap() {
        post {
            bitmapPath?.let {
                context.getBitmapFromPath(it, viewWidth)?.let { bitmap ->
                    setupBitmap(bitmap, width, height)
                    postInvalidate()
                }
            }
            bitmapUri?.let {
                context.getBitmapFromUri(it, viewWidth)?.let { bitmap ->
                    setupBitmap(bitmap, width, height)
                    postInvalidate()
                }
            }
        }
    }

    private fun setupBitmap(bitmap: Bitmap, w: Int, h: Int) {
        bitmapRectF.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val currBitmapWidth = bitmapRectF.width() * currScaleX()
        if (currBitmapWidth < viewWidth) {
            val initTranslateX = (viewWidth - currBitmapWidth) * 0.5f
            val dx = initTranslateX - currTranslateX()
            bitmapMatrix.postTranslate(dx, 0f)
        } else {
            val scaleFactor = viewWidth.toFloat() / bitmap.width.toFloat()
            initScaleX = scaleFactor
            initScaleY = scaleFactor
            bitmapMatrix.postScale(scaleFactor, scaleFactor)
        }
        val currBitmapHeight = bitmapRectF.height() * currScaleY()
        if (currBitmapHeight < viewHeight) {
            val initTranslateY = (viewHeight - currBitmapHeight) * 0.5f
            val dy = initTranslateY - currTranslateY()
            bitmapMatrix.postTranslate(0f, dy)
        }
        val mosaicWidth = bitmap.width / MOSAIC_COEFFICIENT
        val mosaicHeight = bitmap.height / MOSAIC_COEFFICIENT
        mosaicBitmap = bitmap.scale(mosaicWidth, mosaicHeight, false)
        mosaicLayer.setParentBitmap(bitmap)
        mosaicLayer.onSizeChanged(w, h, bitmap.width, bitmap.height)
        graffitiLayer.onSizeChanged(w, h, bitmap.width, bitmap.height)
        computeBinRectF()
    }

}
