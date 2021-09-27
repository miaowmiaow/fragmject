package com.example.fragment.library.base.view

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * 轻量级无痕埋点布局，使用方式android:tag="bury://埋点信息"
 */
class BuryPointLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object{
        const val BURY = "bury://"
    }

    private var listener: OnBuryPointListener? = null

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            findTouchView(event.rawX, event.rawY, this)
        }
        return super.onInterceptTouchEvent(event)
    }

    private fun findTouchView(x: Float, y: Float, view: View): View? {
        if (isTouchInView(x, y, view)) {
            if (isBuryPointView(view)) {
                return view
            }
            if (view is ViewGroup) {
                val size = 0 until view.childCount
                for (i in size.reversed()) {
                    val targetView = findTouchView(x, y, view.getChildAt(i))
                    if (targetView != null) {
                        return targetView
                    }
                }
            }
        }
        return null
    }

    private fun isTouchInView(x: Float, y: Float, targetView: View): Boolean {
        val rect = Rect()
        if (targetView.getGlobalVisibleRect(rect)) {
            val rectF = RectF(rect)
            if (rectF.contains(x, y)) {
                return true
            }
        }
        return false
    }

    private fun isBuryPointView(view: View): Boolean {
        if (view.tag != null) {
            val tag = view.tag.toString()
            if (tag.startsWith(BURY)) {
                val buryStr = tag.substring(BURY.length)
                listener?.onBuryPoint(view, buryStr)
                return true
            }
        }
        return false
    }

    fun setOnBuryPointListener(listener: OnBuryPointListener) {
        this.listener = listener
    }

    interface OnBuryPointListener {
        fun onBuryPoint(view: View, buryStr: String)
    }

}