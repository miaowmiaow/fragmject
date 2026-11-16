package com.example.fragmject.feature.authenticator.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.util.WeakHashMap

/**
 * 在当前窗口上设置 FLAG_SECURE：截屏、录屏与多任务缩略图都会得到黑屏。
 * 用于展示验证码、密钥或种子二维码的页面与对话框。
 *
 * 作用域：在对话框内容中调用时作用于**对话框自己的窗口**（系统对话框是独立窗口，
 * 不会继承 Activity 窗口的 FLAG_SECURE）；否则作用于所在 Activity 窗口。
 *
 * 引用计数：同一窗口上多个调用方同时要求时，只有最后一个离开才清除标记。
 * 仅在主线程使用（组合与 DisposableEffect 都在主线程）。
 *
 * 这是应用安全（防止数据经屏幕泄露），与加固（反逆向 / 反篡改）无关。
 */
@Composable
internal fun SecureWindowEffect() {
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(view, context) {
        val window = findDialogWindow(view) ?: context.findActivity()?.window
        if (window != null) acquireSecure(window)
        onDispose {
            if (window != null) releaseSecure(window)
        }
    }
}

private val secureRefCounts = WeakHashMap<Window, Int>()

private fun acquireSecure(window: Window) {
    val count = secureRefCounts[window] ?: 0
    secureRefCounts[window] = count + 1
    if (count == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
}

private fun releaseSecure(window: Window) {
    val count = secureRefCounts[window] ?: return
    if (count > 1) {
        secureRefCounts[window] = count - 1
    } else {
        secureRefCounts.remove(window)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

/** 从当前 View 向上查找所属的对话框窗口；不在对话框内返回 null。 */
private fun findDialogWindow(view: View): Window? {
    var current: View? = view
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent as? View
    }
    return null
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
