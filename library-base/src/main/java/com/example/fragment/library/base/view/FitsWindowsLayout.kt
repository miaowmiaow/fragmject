package com.example.fragment.library.base.view

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Secure
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.view.Window
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
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
            statusBarColor = getColor(
                R.styleable.FitsWindowsLayout_status_bar_color, statusBarColor
            )
            statusBarFits = getBoolean(
                R.styleable.FitsWindowsLayout_status_bar_fits, statusBarFits
            )
            statusBarLight = getBoolean(
                R.styleable.FitsWindowsLayout_status_bar_light, statusBarLight
            )
            navigationBarColor = getColor(
                R.styleable.FitsWindowsLayout_navigation_bar_color, navigationBarColor
            )
            navigationBarFits = getBoolean(
                R.styleable.FitsWindowsLayout_navigation_bar_fits, navigationBarFits
            )
            navigationBarLight = getBoolean(
                R.styleable.FitsWindowsLayout_navigation_bar_light, navigationBarLight
            )
            recycle()
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setDecorFitsSystemWindows(false)
        setStatusBar(statusBarColor, statusBarLight)
        setNavigationBar(navigationBarColor, navigationBarLight)
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
     * @param decorFitsSystemWindows true:不沉浸，false:沉浸
     */
    fun setDecorFitsSystemWindows(decorFitsSystemWindows: Boolean){
        WindowCompat.setDecorFitsSystemWindows(window, decorFitsSystemWindows)
    }

    /**
     * 设置状态栏
     */
    fun setStatusBar(@ColorInt color: Int, isLight: Boolean) {
        statusBar.setBackgroundColor(color) //设置状态栏底色
        WindowCompat.getInsetsController(window, this)?.apply {
            isAppearanceLightStatusBars = isLight //设置状态栏亮起
        }
    }

    /**
     * 设置导航栏
     */
    fun setNavigationBar(@ColorInt color: Int, isLight: Boolean) {
        navigationBar.setBackgroundColor(color) //设置导航栏底色
        WindowCompat.getInsetsController(window, this)?.apply {
            isAppearanceLightNavigationBars = isLight //设置导航栏亮起
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
    if (hasNavigationBar()) {
        val res = context.resources
        val showId = res.getIdentifier("config_showNavigationBar", "bool", "android")
        if (showId > 0) {
            val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = res.getDimensionPixelSize(resourceId)
            }
        }
    }
    return result
}

fun View.hasNavigationBar() = when {
    checkIsHuaweiRom() && isHuaWeiHideNav() -> false
    checkIsVivoRom() && isVivoFullScreen() -> false
    else -> true
}

/**
 * 华为手机是否隐藏了虚拟导航栏
 * @return true 表示隐藏了，false 表示未隐藏
 */
fun View.isHuaWeiHideNav(): Boolean {
    return Settings.Global.getInt(context.contentResolver, "navigationbar_is_min", 0) != 0
}

/**
 * Vivo手机是否开启手势操作
 * @return true 表示使用的是手势，false 表示使用的是虚拟导航栏(NavigationBar)，默认是false
 */
fun View.isVivoFullScreen(): Boolean {
    return Secure.getInt(context.contentResolver, "navigation_gesture_on", 0) != 0
}

fun checkIsHuaweiRom() = Build.MANUFACTURER.contains("HUAWEI")

fun checkIsVivoRom(): Boolean {
    return Build.MANUFACTURER.contains("VIVO") || Build.MANUFACTURER.contains("vivo")
}
