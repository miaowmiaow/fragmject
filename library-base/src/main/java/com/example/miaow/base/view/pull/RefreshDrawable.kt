package com.example.miaow.base.view.pull

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import com.example.miaow.base.R

class RefreshDrawable(private val parentView: PullRefreshLayout) : Drawable(), Animatable {

    companion object {
        private const val ANIMATION_DURATION = 2500L
        private const val ROTATE_FACTOR = 0.25
    }

    private var loadingBitmaps = listOf<Bitmap>(
        BitmapFactory.decodeResource(parentView.resources, R.drawable.loading_big_1),
        BitmapFactory.decodeResource(parentView.resources, R.drawable.loading_big_4),
        BitmapFactory.decodeResource(parentView.resources, R.drawable.loading_big_7),
        BitmapFactory.decodeResource(parentView.resources, R.drawable.loading_big_10),
        BitmapFactory.decodeResource(parentView.resources, R.drawable.loading_big_13),
        BitmapFactory.decodeResource(parentView.resources, R.drawable.loading_big_16),
        BitmapFactory.decodeResource(parentView.resources, R.drawable.loading_big_19)
    )

    private var bitmapWidth = loadingBitmaps[0].width
    private var bitmapHeight = loadingBitmaps[0].height

    private val screenWidth = parentView.resources.displayMetrics.widthPixels.toFloat()
    private var offsetX = (screenWidth - bitmapWidth) / 2f
    private var offsetY = parentView.getMaxDragDistance() - (bitmapHeight / 2f)
    private var percent = 0L

    private var animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            invalidate()
        }
    }

    init {
        animation.repeatCount = Animation.INFINITE
        animation.repeatMode = Animation.RESTART
        animation.interpolator = LinearInterpolator()
        animation.duration = ANIMATION_DURATION
    }

    fun invalidate() {
        percent += 1
        invalidateSelf()
    }

    fun offsetTopAndBottom(offset: Int) {
        offsetY -= offset / 2f
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val size = loadingBitmaps.size - 1
        val index = (percent * ROTATE_FACTOR % size).toInt()
        canvas.drawBitmap(loadingBitmaps[index], offsetX, offsetY, null)
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun start() {
        animation.reset()
        parentView.startAnimation(animation)
    }

    override fun stop() {
        parentView.clearAnimation()
        percent = 0L
    }

    override fun isRunning(): Boolean {
        return false
    }

}