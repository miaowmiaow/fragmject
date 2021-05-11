package com.example.fragment.library.base.component.view

import android.R
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.example.fragment.library.base.utils.StatusBarUtils

class SimpleStatusBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        val activity = context as Activity
        setTranslucentStatus(activity)
        setStatusBarTheme(activity, false)
    }

    fun setStatusBarTheme(activity: Activity, darkTheme: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val decorView: View = activity.window.decorView
                if (decorView != null) {
                    var vis = decorView.systemUiVisibility
                    vis = if (darkTheme) {
                        vis or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    } else {
                        vis and SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    }
                    if (decorView.systemUiVisibility != vis) {
                        decorView.systemUiVisibility = vis
                    }
                }
            } else {
                if (!darkTheme) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val window = activity.window
                        window.statusBarColor = 0x33000000
                    } else {
                        setTranslucentStatus(activity)
                        setBackgroundColor(0x33000000);
                    }
                }
            }
        }
        return false
    }

    fun setRootViewFitsSystemWindows(activity: Activity, fitSystemWindows: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val winContent = activity.findViewById<View>(R.id.content) as ViewGroup
            if (winContent.childCount > 0) {
                val rootView = winContent.getChildAt(0) as ViewGroup
                if (rootView != null) {
                    rootView.fitsSystemWindows = fitSystemWindows
                }
            }
        }
    }

    private fun setTranslucentStatus(activity: Activity) {
        val window: Window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
            val decorView: View = window.decorView
            //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
            val option = (SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE)
            decorView.systemUiVisibility = option
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val attributes: WindowManager.LayoutParams = window.attributes
            val flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            attributes.flags = attributes.flags or flagTranslucentStatus
            window.attributes = attributes
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(widthSpecSize, StatusBarUtils.getStatusBarHeight(context))
    }

}