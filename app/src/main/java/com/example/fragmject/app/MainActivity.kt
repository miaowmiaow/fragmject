package com.example.fragmject.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.feature.article.WebViewPool
import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.designsystem.rememberWindowSizeClass
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.common.debug.DebugBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var webViewPool: WebViewPool

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
            val darkTheme by themeRepository.observeDarkTheme()
                .collectAsStateWithLifecycle(initialValue = false)
            AppTheme(window = window, darkTheme = darkTheme) {
                rememberWindowSizeClass {
                    AppNavGraph()
                }
            }
        }
        // WebView 预创建（内部已在主线程 IdleHandler 中执行，不阻塞首帧）
        webViewPool.prepare(applicationContext)
        // 仅在 Debug 构建中启用 WebView 调试，避免在 Release 包暴露调试接口
        if (DebugBridge.allowWebContentsDebugging) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}

/**
 * AnimatorSet 没有 KTX 的 doOnEnd，简单适配一下，避免引入额外依赖。
 */
private inline fun AnimatorSet.doOnEndCompat(crossinline action: () -> Unit) {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            action()
        }

        // 动画被取消时（如配置变更）不会再收到 onAnimationEnd，必须同样移除 SplashScreen
        override fun onAnimationCancel(animation: Animator) {
            action()
        }
    })
}