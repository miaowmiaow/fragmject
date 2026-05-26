package com.example.fragment.project

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.fragment.project.ui.web.WebViewManager
import com.example.miaow.base.debug.DebugBridge

class WanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // 自定义退出过渡：150ms alpha + 轻微缩放，避免 splash 与首页之间的"硬切"
        splashScreen.setOnExitAnimationListener { provider ->
            val splashView = provider.view
            val alpha = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f)
            val scaleX = ObjectAnimator.ofFloat(splashView, View.SCALE_X, 1f, 1.2f)
            val scaleY = ObjectAnimator.ofFloat(splashView, View.SCALE_Y, 1f, 1.2f)
            AnimatorSet().apply {
                interpolator = AccelerateInterpolator()
                duration = 150L
                playTogether(alpha, scaleX, scaleY)
                doOnEndCompat { provider.remove() }
                start()
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WanTheme(window) {
                WanNavGraph()
            }
        }
        // WebView 预创建（内部已在主线程 IdleHandler 中执行，不阻塞首帧）
        WebViewManager.prepare(applicationContext)
        // 仅在 Debug 构建中启用 WebView 调试，避免在 Release 包暴露调试接口
        if (DebugBridge.allowWebContentsDebugging) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 数据库随进程生命周期管理，无需在 Activity 销毁时关闭
        WebViewManager.destroy()
    }
}

/**
 * AnimatorSet 没有 KTX 的 doOnEnd，简单适配一下，避免引入额外依赖。
 */
private inline fun AnimatorSet.doOnEndCompat(crossinline action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) {
            action()
        }
    })
}