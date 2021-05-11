package com.example.fragment.library.base.component.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.Scroller
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

open class SimpleSlideLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val ANIMATION_TIME = 1000
    }

    private var downX = 0
    private var downY = 0
    private var viewHeight = 0

    private var isSliding = false
    private var isFinish = false

    private var mScroller: Scroller = Scroller(context)
    private var mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        visibility = GONE
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX.toInt()
                downY = event.rawY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.rawX.toInt()
                val moveY = event.rawY.toInt()
                if (downY - moveY > mTouchSlop && abs(downX - moveX) < mTouchSlop) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }


    @Override
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.rawX.toInt()
                val moveY = event.rawY.toInt()
                if (downY - moveY > mTouchSlop && abs(downX - moveX) < mTouchSlop) {
                    isSliding = true
                }
                if (downY - moveY > 0 && isSliding) {
                    scrollTo(0, downY - moveY)
                } else {
                    scrollTo(0, 0)
                }
            }
            MotionEvent.ACTION_UP -> {
                isSliding = false
                if (scrollY >= viewHeight / 3) {
                    isFinish = true
                    scrollTop()
                } else {
                    scrollOrigin()
                    isFinish = false
                }
            }
        }
        return true
    }

    @Override
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (changed) {
            viewHeight = this.height
        }
    }

    private fun scrollTop() {
        val delta = viewHeight - scrollY
        mScroller.startScroll(0, scrollY, 0, delta, ANIMATION_TIME)
        postInvalidate()
    }

    private fun scrollOrigin() {
        val delta = scrollY
        mScroller.startScroll(0, scrollY, 0, -delta, ANIMATION_TIME)
        postInvalidate()
    }

    fun show() {
        visibility = VISIBLE
        scrollOrigin()
        postDelayed({ dismiss() }, 3000)
    }

    fun dismiss() {
        scrollTop()
        postDelayed({ visibility = GONE }, ANIMATION_TIME.toLong())
    }

    @Override
    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.currX, mScroller.currY)
            postInvalidate()
            if (mScroller.isFinished && isFinish) {
                if (onFinishListener != null) {
                    isFinish = false
                    onFinishListener!!.onFinish()
                }
            }
        }
    }

    private var onFinishListener: OnFinishListener? = null

    fun setOnFinishListener(onFinishListener: OnFinishListener) {
        this.onFinishListener = onFinishListener
    }

    interface OnFinishListener {
        fun onFinish()
    }

}