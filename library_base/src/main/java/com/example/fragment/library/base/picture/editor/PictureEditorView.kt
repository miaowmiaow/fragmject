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
import com.example.fragment.library.base.picture.editor.bean.EditorMode
import com.example.fragment.library.base.picture.editor.layer.GraffitiLayer
import com.example.fragment.library.base.picture.editor.layer.MosaicLayer
import com.example.fragment.library.base.picture.editor.layer.StickerLayer
import java.util.*

class PictureEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val INVALID_ID = -1
        private const val MOSAIC_COEFFICIENT = 36
        private const val DELETE_AREA_WIDTH = 300
        private const val DELETE_AREA_HEIGHT = 200
        private const val RECT_ROUND = 30.0f
    }

    private val layerMatrix = Matrix()
    private val layerMatrixValues = FloatArray(9)
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
    private var bitmapPath = ""
    private val bitmapRectF = RectF()
    private val bitmapOptions = BitmapFactory.Options()
    private var mosaicBitmap: Bitmap? = null
    private var deleteIcon: Bitmap? = null

    private val mosaicLayer = MosaicLayer(this)
    private val graffitiLayer = GraffitiLayer(this)
    private val stickerLayers = Stack<StickerLayer>()
    private var stickerLayerIndex = INVALID_ID
    private var pointerIndexId0 = INVALID_ID

    private val deleteAreaPaint = Paint()
    private val deleteAreaRectF = RectF()
    private val deleteIconRectF = RectF()
    private var drawDeleteArea = false

    private val scroller = Scroller(context)
    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.OnGestureListener {

            override fun onDown(e: MotionEvent?): Boolean {
                if (!scroller.isFinished) {
                    scroller.forceFinished(true)
                }
                return false
            }

            override fun onShowPress(e: MotionEvent?) {
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return false
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent?,
                distanceX: Float, distanceY: Float
            ): Boolean {
                onScroll(-distanceX, -distanceY)
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent?,
                velocityX: Float, velocityY: Float
            ): Boolean {
                setMatrixValues()
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
        })

    private val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                setMatrixValues()
                if (currOffsetX > initTranslateX) {
                    layerMatrix.postTranslate(-(currOffsetX - initTranslateX), 0f)
                }
                if (currOffsetY > initTranslateY) {
                    layerMatrix.postTranslate(0f, -(currOffsetY - initTranslateY))
                }
                if (currScaleX * detector.scaleFactor > 1f && currScaleY * detector.scaleFactor > 1f) {
                    layerMatrix.postScale(
                        detector.scaleFactor, detector.scaleFactor, detector.focusX, detector.focusY
                    )
                    val maxOffsetX = currOffsetX + bitmapWidth * (currScaleX * detector.scaleFactor)
                    if (maxOffsetX < viewWidth - initTranslateX) {
                        layerMatrix.postTranslate(viewWidth - initTranslateX - maxOffsetX, 0f)
                    }
                    val maxOffsetY =
                        currOffsetY + bitmapHeight * (currScaleY * detector.scaleFactor)
                    if (maxOffsetY < viewHeight - initTranslateY) {
                        layerMatrix.postTranslate(0f, viewHeight - initTranslateY - maxOffsetY)
                    }
                } else {
                    layerMatrix.postScale(
                        1 / currScaleX, 1 / currScaleY, detector.focusX, detector.focusY
                    )
                    val maxOffsetX = currOffsetX + bitmapWidth
                    if (maxOffsetX < viewWidth - initTranslateX) {
                        layerMatrix.postTranslate(viewWidth - initTranslateX - maxOffsetX, 0f)
                    }
                    val maxOffsetY = currOffsetY + bitmapHeight
                    if (maxOffsetY < viewHeight - initTranslateY) {
                        layerMatrix.postTranslate(0f, viewHeight - initTranslateY - maxOffsetY)
                    }
                }
                invalidate()
                return true
            }

        })

    init {
        deleteAreaPaint.isAntiAlias = true
        deleteAreaPaint.color = Color.GRAY
        deleteAreaPaint.style = Paint.Style.FILL
        deleteIcon = BitmapFactory.decodeResource(resources, R.drawable.pe_ashbin)
    }

    fun setBitmapResource(path: String) {
        this.bitmapPath = path
        invalidate()
    }

    fun setMode(editorMode: EditorMode) {
        if (editorMode == EditorMode.MOSAIC) {
            mosaicLayer.isEnabled = true
            graffitiLayer.isEnabled = false
            stickerLayers.forEach { layer ->
                layer.isEnabled = false
            }
        } else if (editorMode == EditorMode.GRAFFITI || editorMode == EditorMode.ERASER) {
            graffitiLayer.setPaintMode(editorMode)
            mosaicLayer.isEnabled = false
            graffitiLayer.isEnabled = true
            stickerLayers.forEach { layer ->
                layer.isEnabled = false
            }
        } else if (editorMode == EditorMode.STICKER) {
            mosaicLayer.isEnabled = false
            graffitiLayer.isEnabled = false
            stickerLayers.forEach { layer ->
                layer.isEnabled = true
            }
        }
    }

    fun setGraffitiColor(@ColorInt color: Int) {
        graffitiLayer.setPaintColor(color)
    }

    fun setStickerBitmap(
        bitmap: Bitmap,
        contentDescription: String,
        parentTouchX: Float,
        parentTouchY: Float
    ) {
        val stickerWidth = bitmap.width
        val stickerHeight = bitmap.height
        val options = BitmapFactory.Options()
        options.inScaled = true
        if (stickerWidth > stickerHeight) {
            options.inDensity = stickerWidth
            options.inTargetDensity = (viewWidth / currScaleX * 0.25).toInt()
        } else {
            options.inDensity = stickerHeight
            options.inTargetDensity = (viewWidth / currScaleX * 0.25).toInt()
        }
        val stickerLayer = StickerLayer(this)
        stickerLayer.setOnClickListener(object : StickerLayer.OnClickListener {
            override fun onClick(
                bitmap: Bitmap?,
                contentDescription: String,
                parentTouchX: Float,
                parentTouchY: Float
            ) {
                if (stickerLayerIndex != INVALID_ID) {
                    stickerLayers.remove(stickerLayers[stickerLayerIndex])
                }
                stickerLayerIndex = INVALID_ID
                onStickerClick?.onStickerClick(
                    bitmap, contentDescription, parentTouchX, parentTouchY
                )
            }
        })
        stickerLayer.onSizeChanged(viewWidth, viewHeight)
        stickerLayer.setBitmap(bitmap, contentDescription, parentTouchX, parentTouchY)
        stickerLayers.push(stickerLayer)
    }

    fun graffitiUndo() {
        graffitiLayer.undo()
    }

    fun mosaicUndo() {
        mosaicLayer.undo()
    }

    fun conversionBitmap(): Bitmap {
        layerMatrix.reset()
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        draw(canvas)
        return bitmap
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerId0 = event.getPointerId(0)
        pointerIndexId0 = event.findPointerIndex(pointerId0)
        setMatrixValues()
        val layerEvent = MotionEvent.obtain(event)
        val newX = (event.x - currOffsetX) / currScaleX
        val newY = (event.y - currOffsetY) / currScaleY
        layerEvent.setLocation(newX, newY)
        if (stickerLayerIndex == INVALID_ID) {
            for (i in 0 until stickerLayers.size) {
                if (stickerLayers[i].inTextBounds(newX, newY)) {
                    stickerLayerIndex = i
                    break
                }
            }
        }
        if (stickerLayerIndex != INVALID_ID) {
            val layer = stickerLayers[stickerLayerIndex]
            if (layer.onTouchEvent(layerEvent)) {
                layer.parentTouchX = layerEvent.x
                layer.parentTouchY = layerEvent.y
                drawDeleteArea = true
                if (deleteAreaRectF.contains(layerEvent.x, layerEvent.y)) {
                    deleteAreaPaint.color = Color.RED
                } else {
                    deleteAreaPaint.color = Color.GRAY
                }
                layerEvent.recycle()
                return true
            }
        }
        if (graffitiLayer.onTouchEvent(layerEvent)) {
            layerEvent.recycle()
            return true
        }
        if (mosaicLayer.onTouchEvent(layerEvent)) {
            layerEvent.recycle()
            return true
        }
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            drawDeleteArea = false
            if (stickerLayerIndex != INVALID_ID && event.getPointerId(0) == pointerIndexId0) {
                if (deleteAreaRectF.contains(layerEvent.x, layerEvent.y)) {
                    stickerLayers.remove(stickerLayers[stickerLayerIndex])
                }
            }
            stickerLayerIndex = INVALID_ID
        }
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
            layerMatrix.reset()
            if (bitmapWidth <= viewWidth) {
                initTranslateX = (viewWidth - bitmapWidth) * 0.5f
                if (bitmapHeight < viewHeight) {
                    initTranslateY = (viewHeight - bitmapHeight) * 0.5f
                }
                layerMatrix.postTranslate(initTranslateX, initTranslateY)
            }
            val mosaicWidth = bitmapWidth / MOSAIC_COEFFICIENT
            val mosaicHeight = bitmapHeight / MOSAIC_COEFFICIENT
            mosaicBitmap = Bitmap.createScaledBitmap(bitmap, mosaicWidth, mosaicHeight, false)
            mosaicLayer.setParentBitmap(bitmap)
            mosaicLayer.onSizeChanged(bitmapWidth, bitmapHeight)
            graffitiLayer.onSizeChanged(bitmapWidth, bitmapHeight)
        }
        setDeleteArea()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mosaicBitmap?.let {
            canvas.setMatrix(layerMatrix)
            canvas.drawBitmap(it, null, bitmapRectF, null)
        }
        mosaicLayer.onDraw(canvas)
        graffitiLayer.onDraw(canvas)
        stickerLayers.forEach { layer ->
            layer.onDraw(canvas)
        }
        drawDeleteArea(canvas)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (!scroller.isFinished && scroller.computeScrollOffset()) {
            val currX = scroller.currX
            val currY = scroller.currY
            onScroll(-(currX - currScrollX), -(currY - currScrollY))
            currScrollX = currX.toFloat()
            currScrollY = currY.toFloat()
        }
    }

    private fun onScroll(dx: Float, dy: Float) {
        setMatrixValues()
        if (currOffsetX + dx <= 0 && currOffsetX + dx >= -(bitmapWidth * currScaleX - viewWidth)) {
            layerMatrix.postTranslate(dx, 0f)
        }
        if (currOffsetY + dy <= 0 && currOffsetY + dy >= -(bitmapHeight * currScaleY - viewHeight)) {
            layerMatrix.postTranslate(0f, dy)
        }
        invalidate()
    }

    private fun setMatrixValues() {
        layerMatrix.getValues(layerMatrixValues)
        currScaleX = layerMatrixValues[0]
        currScaleY = layerMatrixValues[4]
        currOffsetX = layerMatrixValues[2]
        currOffsetY = layerMatrixValues[5]
        graffitiLayer.setPaintStrokeWidthScale(currScaleX)
        setDeleteArea()
    }

    private fun drawDeleteArea(canvas: Canvas) {
        if (drawDeleteArea) {
            canvas.drawRoundRect(
                deleteAreaRectF,
                RECT_ROUND / currScaleX,
                RECT_ROUND / currScaleX,
                deleteAreaPaint
            )
            deleteIcon?.let {
                canvas.drawBitmap(it, null, deleteIconRectF, null)
            }
        }
    }

    private fun setDeleteArea() {
        deleteAreaPaint.isAntiAlias = true
        deleteAreaPaint.color = Color.GRAY
        deleteAreaPaint.style = Paint.Style.FILL
        val left = ((viewWidth - DELETE_AREA_WIDTH) * 0.5f - currOffsetX) / currScaleX
        val top = ((viewHeight - DELETE_AREA_HEIGHT - RECT_ROUND) - currOffsetY) / currScaleY
        val right = left + DELETE_AREA_WIDTH / currScaleX
        val bottom = top + DELETE_AREA_HEIGHT / currScaleX
        deleteAreaRectF.set(left, top, right, bottom)
        deleteIconRectF.set(
            left + DELETE_AREA_WIDTH * 0.30f / currScaleX,
            top + DELETE_AREA_HEIGHT * 0.30f / currScaleX,
            right - DELETE_AREA_WIDTH * 0.30f / currScaleX,
            bottom - DELETE_AREA_HEIGHT * 0.30f / currScaleX
        )
    }

    private var onStickerClick: OnStickerClickListener? = null

    fun setOnStickerClickListener(onStickerClick: OnStickerClickListener) {
        this.onStickerClick = onStickerClick
    }

    interface OnStickerClickListener {
        fun onStickerClick(
            bitmap: Bitmap?,
            contentDescription: String,
            parentTouchX: Float,
            parentTouchY: Float
        )
    }
}