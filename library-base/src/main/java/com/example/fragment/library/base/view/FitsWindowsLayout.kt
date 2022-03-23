package com.example.fragment.library.base.view

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.view.Window
import android.widget.RelativeLayout
import androidx.core.view.WindowCompat
import com.example.fragment.library.base.R

/**
 * 沉浸式布局
 */
class FitsWindowsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val window: Window = try {
        (context as Activity).window
    } catch (e: Exception) {
        ((context as ContextThemeWrapper).baseContext as Activity).window
    }

    private val statusBar = View(context).apply {
        id = R.id.status_bar_view
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, statusBarHeight()).apply {
            addRule(ALIGN_PARENT_TOP)
        }
    }

    private val navigationBar = View(context).apply {
        id = R.id.navigation_bar_view
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, navigationBarHeight()).apply {
            addRule(ALIGN_PARENT_BOTTOM)
        }
    }

    //状态栏
    private var statusBarColor = Color.TRANSPARENT
    private var statusBarFits = false
    private var statusBarLight = false

    //导航栏沉浸
    private var navigationBarColor = Color.TRANSPARENT
    private var navigationBarFits = false
    private var navigationBarLight = true

    init {
        context.obtainStyledAttributes(attrs, R.styleable.FitsWindowsLayout).apply {
            statusBarColor = getColor(R.styleable.FitsWindowsLayout_status_bar_color, statusBarColor)
            statusBarFits = getBoolean(R.styleable.FitsWindowsLayout_status_bar_fits, statusBarFits)
            statusBarLight = getBoolean(R.styleable.FitsWindowsLayout_status_bar_light, statusBarLight)
            navigationBarColor = getColor(R.styleable.FitsWindowsLayout_navigation_bar_color, navigationBarColor)
            navigationBarFits = getBoolean(R.styleable.FitsWindowsLayout_navigation_bar_fits, navigationBarFits)
            navigationBarLight = getBoolean(R.styleable.FitsWindowsLayout_navigation_bar_light, navigationBarLight)
            recycle()
        }
        setDecorFitsSystemWindows(window)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount == 1) {
            getChildAt(0).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                if (!statusBarFits) {
                    addView(statusBar)
                    (layoutParams as LayoutParams).addRule(BELOW, R.id.status_bar_view)
                }
                if (!navigationBarFits) {
                    addView(navigationBar)
                    (layoutParams as LayoutParams).addRule(ABOVE, R.id.navigation_bar_view)
                }
            }
        } else {
            throw IllegalArgumentException("FitsWindowsLayout can only have one child")
        }
    }

    /**
     * 设置沉浸模式
     */
    fun setDecorFitsSystemWindows(window: Window) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false) //true:不沉浸，false:沉浸
        statusBar.setBackgroundColor(statusBarColor) //设置状态栏底色
        navigationBar.setBackgroundColor(navigationBarColor) //设置导航栏底色
        WindowCompat.getInsetsController(window, this)?.apply {
            isAppearanceLightStatusBars = statusBarLight //设置状态栏亮起
            isAppearanceLightNavigationBars = navigationBarLight //设置导航栏亮起
        }
    }

}

fun View.statusBarHeight(): Int {
    var result = 0
    context.resources.apply {
        val resourceId = getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = getDimensionPixelSize(resourceId)
        }
    }
    return result
}

fun View.navigationBarHeight(): Int {
    var result = 0
    context.resources.apply {
        val showId = getIdentifier("config_showNavigationBar", "bool", "android")
        if (showId > 0) {
            val resourceId = getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = getDimensionPixelSize(resourceId)
            }
        }
    }
    return result
}