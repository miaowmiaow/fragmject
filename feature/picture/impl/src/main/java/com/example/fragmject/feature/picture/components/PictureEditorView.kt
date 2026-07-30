package com.example.fragmject.feature.picture.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.values
import com.example.fragmject.core.ui.utils.getBitmapFromPath
import com.example.fragmject.core.ui.utils.getBitmapFromUri
import com.example.fragmject.feature.picture.impl.R
import com.example.fragmject.feature.picture.components.layer.GraffitiLayer
import com.example.fragmject.feature.picture.components.layer.MosaicLayer
import com.example.fragmject.feature.picture.components.layer.OnStickerClickListener
import com.example.fragmject.feature.picture.components.layer.StickerLayer
import com.example.fragmject.feature.picture.model.StickerAttrs
import java.util.Stack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

enum class EditorMode { GRAFFITI, MOSAIC, STICKER }

@Stable
class PictureEditorState {
    var viewSize by mutableStateOf(IntSize.Zero)
    private var _mode by mutableStateOf(EditorMode.STICKER)
    var isBin by mutableStateOf(false)
    var refreshTrigger by mutableLongStateOf(0L)

    private val bitmapMatrix = Matrix()
    private val bitmapRectF = RectF()
    private var mosaicBitmap: Bitmap? = null
    val mosaicLayer = MosaicLayer(this)
    val graffitiLayer = GraffitiLayer(this)
    val stickerLayers = Stack<StickerLayer>()
    private var stickerLayerIndex = INVALID_ID

    private val binIcon: Bitmap
    private val binPaint = Paint()
    private val binRectF = RectF()
    private val binIconRectF = RectF()
    private val binTextPaint = Paint()
    private var binTextWidth = 0f
    private var binTextSize = BIN_TEXT_SIZE
    private var binTextBaselineY = 0f
    private var binTextX = 0f
    private var binTextY = 0f

    private var isDoubleTap = false
    private var lastTapTime = 0L
    private var initScaleX = 1f
    private var initScaleY = 1f

    private var bitmapPath: String? = null
    private var bitmapUri: Uri? = null

    val context: Context

    constructor(context: Context) {
        this.context = context
        binIcon = BitmapFactory.decodeResource(context.resources, R.drawable.pe_bin)
        binPaint.style = Paint.Style.FILL
        binTextPaint.color = Color.WHITE
        binTextPaint.textSize = binTextSize
        binTextPaint.style = Paint.Style.STROKE
        binTextWidth = binTextPaint.measureText(BIN_TEXT)
        binTextBaselineY = abs(binTextPaint.ascent() + binTextPaint.descent()) * 0.5f
    }

    fun invalidate() {
        refreshTrigger++
    }

    fun currScaleX() = bitmapMatrix.values()[0]
    fun currScaleY() = bitmapMatrix.values()[4]
    fun currTranslateX() = bitmapMatrix.values()[2]
    fun currTranslateY() = bitmapMatrix.values()[5]

    fun setBitmapPathOrUri(path: String?, uri: Uri?) {
        this.bitmapPath = path
        this.bitmapUri = uri
        if (viewSize.width > 0) initBitmap()
    }

    fun setMode(mode: EditorMode) {
        this._mode = mode
        graffitiLayer.isEnabled = mode == EditorMode.GRAFFITI
        mosaicLayer.isEnabled = mode == EditorMode.MOSAIC
        stickerLayers.forEach { it.isEnabled = mode == EditorMode.STICKER }
    }

    fun setGraffitiColor(color: Int) {
        graffitiLayer.setPaintColor(color)
    }

    fun graffitiUndo() {
        graffitiLayer.undo()
    }

    fun mosaicUndo() {
        mosaicLayer.undo()
    }

    private fun syncStickerLayersParentMatrix() {
        stickerLayers.forEach { it.updateParentMatrix(bitmapMatrix) }
    }

    private fun clearStickerSelection() {
        stickerLayers.forEach { it.setSelected(false) }
    }

    private fun selectSticker(index: Int) {
        stickerLayers.forEachIndexed { stickerIndex, layer ->
            layer.setSelected(stickerIndex == index)
        }
    }

    fun setSticker(attrs: StickerAttrs, listener: OnStickerClickListener? = null) {
        val layer = StickerLayer(this, attrs, object : OnStickerClickListener {
            override fun onClick(attrs: StickerAttrs) {
                if (stickerLayerIndex != INVALID_ID) {
                    stickerLayers.remove(stickerLayers[stickerLayerIndex])
                    stickerLayerIndex = INVALID_ID
                }
                listener?.onClick(attrs)
            }
        })
        clearStickerSelection()
        layer.setSelected(true)
        layer.updateParentMatrix(bitmapMatrix)
        layer.onSizeChanged(
            viewSize.width,
            viewSize.height,
            bitmapRectF.width().toInt(),
            bitmapRectF.height().toInt()
        )
        stickerLayers.push(layer)
        invalidate()
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

    fun draw(canvas: Canvas) {
        canvas.setMatrix(bitmapMatrix)
        mosaicBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, bitmapRectF, null)
            mosaicLayer.onDraw(canvas)
            graffitiLayer.onDraw(canvas)
            stickerLayers.forEach { it.onDraw(canvas) }
        }
        if (isBin) {
            val round = BIN_ROUND / currScaleX()
            canvas.drawRoundRect(binRectF, round, round, binPaint)
            canvas.drawBitmap(binIcon, null, binIconRectF, null)
            binTextPaint.textSize = binTextSize
            canvas.drawText(BIN_TEXT, binTextX, binTextY, binTextPaint)
        }
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        val layerEvent = MotionEvent.obtain(event)
        val layerX = (event.x - currTranslateX()) / currScaleX()
        val layerY = (event.y - currTranslateY()) / currScaleY()
        layerEvent.setLocation(layerX, layerY)

        if (stickerLayerIndex == INVALID_ID) {
            for (index in stickerLayers.indices.reversed()) {
                if (stickerLayers[index].inStickerBounds(layerEvent.x, layerEvent.y)) {
                    stickerLayerIndex = index
                    break
                }
            }
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (stickerLayerIndex != INVALID_ID) {
                    selectSticker(stickerLayerIndex)
                } else {
                    clearStickerSelection()
                    invalidate()
                }
            }
        }

        if (stickerLayerIndex != INVALID_ID && stickerLayers[stickerLayerIndex].onTouchEvent(layerEvent)) {
            isBin = stickerLayers[stickerLayerIndex].shouldShowDeleteBin()
            if (isBin) {
                computeBinRectF()
                val r = if (binRectF.contains(layerX, layerY)) 255 else 0
                binPaint.setARGB(127, r, 0, 0)
            }
            layerEvent.recycle()
            invalidate()
            return true
        }

        if (mosaicLayer.onTouchEvent(layerEvent)) {
            mosaicLayer.setParentScale(currScaleX())
            layerEvent.recycle()
            invalidate()
            return true
        }
        if (graffitiLayer.onTouchEvent(layerEvent)) {
            graffitiLayer.setParentScale(currScaleX())
            layerEvent.recycle()
            invalidate()
            return true
        }

        if (event.actionMasked == MotionEvent.ACTION_UP) {
            if (isBin && stickerLayerIndex != INVALID_ID && binRectF.contains(layerEvent.x, layerEvent.y)) {
                stickerLayers.remove(stickerLayers[stickerLayerIndex])
            }
            isBin = false
            stickerLayerIndex = INVALID_ID
        }
        layerEvent.recycle()
        return false
    }

    fun handleScroll(dx: Float, dy: Float) {
        val currBitmapWidth = bitmapRectF.width() * currScaleX()
        val currBitmapHeight = bitmapRectF.height() * currScaleY()
        var changed = false
        if (currTranslateX() + dx <= 0 && currTranslateX() + dx >= viewSize.width - currBitmapWidth) {
            bitmapMatrix.postTranslate(dx, 0f)
            changed = true
        }
        if (currTranslateY() + dy <= 0 && currTranslateY() + dy >= viewSize.height - currBitmapHeight) {
            bitmapMatrix.postTranslate(0f, dy)
            changed = true
        }
        if (changed) {
            syncStickerLayersParentMatrix()
            invalidate()
        }
    }

    fun handleScale(scaleFactor: Float, focusX: Float, focusY: Float) {
        val sx =
            if (currScaleX() * scaleFactor > initScaleX) scaleFactor else initScaleX / currScaleX()
        val sy =
            if (currScaleY() * scaleFactor > initScaleY) scaleFactor else initScaleY / currScaleY()
        bitmapMatrix.postScale(sx, sy, focusX, focusY)
        resetScaleOffset()
        syncStickerLayersParentMatrix()
        invalidate()
    }

    fun handleDoubleTap(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
            if (isDoubleTap) {
                handleScale(1 / initScaleY / currScaleX(), x, y)
            } else {
                val currBitmapWidth = bitmapRectF.width() * currScaleX()
                handleScale(viewSize.width / currBitmapWidth, x, y)
            }
            isDoubleTap = !isDoubleTap
            lastTapTime = 0L
        } else {
            lastTapTime = now
        }
    }

    private fun resetScaleOffset() {
        val currBitmapWidth = bitmapRectF.width() * currScaleX()
        val dx = if (currBitmapWidth < viewSize.width) {
            (viewSize.width - currBitmapWidth) * 0.5f - currTranslateX()
        } else {
            when {
                currTranslateX() > 0 -> -currTranslateX()
                currTranslateX() + currBitmapWidth < viewSize.width -> viewSize.width - currBitmapWidth - currTranslateX()
                else -> 0f
            }
        }
        val currBitmapHeight = bitmapRectF.height() * currScaleY()
        val dy = if (currBitmapHeight < viewSize.height) {
            (viewSize.height - currBitmapHeight) * 0.5f - currTranslateY()
        } else {
            when {
                currTranslateY() > 0 -> -currTranslateY()
                currTranslateY() + currBitmapHeight < viewSize.height -> viewSize.height - currBitmapHeight - currTranslateY()
                else -> 0f
            }
        }
        bitmapMatrix.postTranslate(dx, dy)
    }

    fun initBitmap() {
        val w = viewSize.width
        val h = viewSize.height
        if (w == 0 || h == 0) return
        bitmapPath?.let { path ->
            context.getBitmapFromPath(path, w)?.let { setupBitmap(it, w, h) }
        }
        bitmapUri?.let { uri ->
            context.getBitmapFromUri(uri, w)?.let { setupBitmap(it, w, h) }
        }
    }

    private fun setupBitmap(bitmap: Bitmap, w: Int, h: Int) {
        bitmapMatrix.reset()
        initScaleX = 1f
        initScaleY = 1f
        isDoubleTap = false
        lastTapTime = 0L
        bitmapRectF.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val currBitmapWidth = bitmapRectF.width() * currScaleX()
        if (currBitmapWidth < w) {
            bitmapMatrix.postTranslate((w - currBitmapWidth) * 0.5f - currTranslateX(), 0f)
        } else {
            val scale = w.toFloat() / bitmap.width.toFloat()
            initScaleX = scale
            initScaleY = scale
            bitmapMatrix.postScale(scale, scale)
        }
        val currBitmapHeight = bitmapRectF.height() * currScaleY()
        if (currBitmapHeight < h) {
            bitmapMatrix.postTranslate(0f, (h - currBitmapHeight) * 0.5f - currTranslateY())
        }
        mosaicBitmap = bitmap.scale(
            bitmap.width / MOSAIC_COEFFICIENT,
            bitmap.height / MOSAIC_COEFFICIENT,
            false
        )
        mosaicLayer.setParentBitmap(bitmap)
        mosaicLayer.onSizeChanged(w, h, bitmap.width, bitmap.height)
        graffitiLayer.onSizeChanged(w, h, bitmap.width, bitmap.height)
        syncStickerLayersParentMatrix()
        computeBinRectF()
        invalidate()
    }

    private fun computeBinRectF() {
        if (viewSize == IntSize.Zero) return
        val density = context.resources.displayMetrics.density
        val reservedBottom = BIN_BOTTOM_RESERVED_DP * density
        val bottomGap = BIN_BOTTOM_GAP_DP * density
        val binLeft = ((viewSize.width - BIN_WIDTH) * 0.5f - currTranslateX()) / currScaleX()
        val binTop = (viewSize.height - reservedBottom - bottomGap - BIN_HEIGHT - currTranslateY()) / currScaleY()
        val binRight = binLeft + BIN_WIDTH / currScaleX()
        val binBottom = binTop + BIN_HEIGHT / currScaleY()
        binRectF.set(binLeft, binTop, binRight, binBottom)
        binIconRectF.set(
            binRectF.centerX() - (BIN_ICON_WIDTH / currScaleX()) * 0.5f,
            binRectF.top + BIN_ROUND / currScaleX(),
            binRectF.centerX() + (BIN_ICON_WIDTH / currScaleX()) * 0.5f,
            binRectF.top + (BIN_ROUND + BIN_ICON_WIDTH) / currScaleY()
        )
        binTextX = binRectF.centerX() - (binTextWidth / currScaleX()) * 0.5f
        binTextY = binRectF.bottom - BIN_ROUND / currScaleY()
        binTextSize = BIN_TEXT_SIZE / currScaleX()
    }

    companion object {
        private const val INVALID_ID = -1
        private const val MOSAIC_COEFFICIENT = 36
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val BIN_WIDTH = 300
        private const val BIN_HEIGHT = 200
        private const val BIN_ROUND = 30
        private const val BIN_ICON_WIDTH = 70
        private const val BIN_TEXT_SIZE = 30f
        private const val BIN_BOTTOM_RESERVED_DP = 160f
        private const val BIN_BOTTOM_GAP_DP = 16f
        private const val BIN_TEXT = "拖动到此处删除"
    }
}

@Composable
fun rememberPictureEditorState(): PictureEditorState {
    val context = LocalContext.current
    return remember { PictureEditorState(context) }
}

@Composable
fun PictureEditorCanvas(
    state: PictureEditorState,
    modifier: Modifier = Modifier,
) {
    var downTime by remember { mutableLongStateOf(0L) }
    var wasPressed by remember { mutableStateOf(false) }
    var prevCentroid by remember { mutableStateOf(Offset.Zero) }
    var prevSpan by remember { mutableFloatStateOf(0f) }
    var prevCount by remember { mutableIntStateOf(0) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                if (state.viewSize == IntSize.Zero && it.width > 0) {
                    state.viewSize = it
                    state.initBitmap()
                } else {
                    state.viewSize = it
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        val first = changes.firstOrNull() ?: continue
                        val currentTime = System.currentTimeMillis()

                        val allPressed = changes.all { it.pressed }
                        val anyPressed = changes.any { it.pressed }

                        val centroid = if (changes.size >= 2) {
                            val positions = changes.map { it.position }
                            Offset(
                                positions.sumOf { it.x.toDouble() }.toFloat() / changes.size,
                                positions.sumOf { it.y.toDouble() }.toFloat() / changes.size
                            )
                        } else {
                            first.position
                        }

                        val span = if (changes.size >= 2) {
                            val p0 = changes[0].position
                            val p1 = changes[1].position
                            val dx = p1.x - p0.x
                            val dy = p1.y - p0.y
                            sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        } else 0f

                        val action = when {
                            !wasPressed && allPressed -> {
                                downTime = currentTime
                                MotionEvent.ACTION_DOWN
                            }
                            wasPressed && !anyPressed -> MotionEvent.ACTION_UP
                            wasPressed && allPressed && changes.size != prevCount -> {
                                if (changes.size > prevCount) MotionEvent.ACTION_POINTER_DOWN
                                else MotionEvent.ACTION_POINTER_UP
                            }
                            wasPressed && allPressed -> MotionEvent.ACTION_MOVE
                            else -> -1
                        }

                        if (action >= 0) {
                            val me = MotionEvent.obtain(
                                downTime, currentTime, action,
                                first.position.x, first.position.y, 0
                            )
                            val handled = state.handleTouchEvent(me)
                            me.recycle()

                            if (!handled && wasPressed && allPressed && changes.size == prevCount) {
                                if (prevCentroid != Offset.Zero && centroid != Offset.Zero) {
                                    val dx = centroid.x - prevCentroid.x
                                    val dy = centroid.y - prevCentroid.y
                                    if (changes.size == 1) {
                                        state.handleScroll(dx, dy)
                                    }
                                }
                                if (span > 0 && prevSpan > 0 && changes.size >= 2) {
                                    val scaleFactor = span / prevSpan
                                    state.handleScale(scaleFactor, centroid.x, centroid.y)
                                }
                            }

                            if (action == MotionEvent.ACTION_DOWN) {
                                state.handleDoubleTap(first.position.x, first.position.y)
                            }
                        }

                        wasPressed = anyPressed
                        prevCentroid = if (anyPressed) centroid else Offset.Zero
                        prevSpan = if (anyPressed && span > 0) span else 0f
                        prevCount = changes.size

                        changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        state.refreshTrigger
        drawIntoCanvas { canvas ->
            state.draw(canvas.nativeCanvas)
        }
    }
}