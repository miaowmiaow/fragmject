package com.example.fragmject.core.qrscanner

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

/** 在预览上绘制识别框与识别到的内容文本。 */
internal class QrCodeDrawable(
    private val qrContent: String,
    private val boundingRect: Rect,
) : Drawable() {

    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.YELLOW
        strokeWidth = 5F
        alpha = 200
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val contentTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val contentPadding = 25
    private val textWidth = contentTextPaint.measureText(qrContent).toInt()

    override fun draw(canvas: Canvas) {
        canvas.drawRect(boundingRect, boundingRectPaint)
        canvas.drawRect(
            Rect(
                boundingRect.left,
                boundingRect.bottom + contentPadding / 2,
                boundingRect.left + textWidth + contentPadding * 2,
                boundingRect.bottom + contentTextPaint.textSize.toInt() + contentPadding,
            ),
            contentRectPaint,
        )
        canvas.drawText(
            qrContent,
            (boundingRect.left + contentPadding).toFloat(),
            (boundingRect.bottom + contentPadding * 2).toFloat(),
            contentTextPaint,
        )
    }

    override fun setAlpha(alpha: Int) {
        boundingRectPaint.alpha = alpha
        contentRectPaint.alpha = alpha
        contentTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        boundingRectPaint.colorFilter = colorFilter
        contentRectPaint.colorFilter = colorFilter
        contentTextPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
