package com.example.fragmject.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import com.example.fragmject.core.webview.WebViewPool
import com.example.fragmject.core.domain.ThemeState
import com.example.fragmject.core.designsystem.rememberWindowSizeClass
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.app.navigation.AppNavGraph
import com.example.fragmject.app.navigation.AppNavigatorBundle
import com.example.fragmject.app.navigation.NavigationDispatcher
import com.example.fragmject.app.navigation.resolveDeepLinkBackStack
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.contract.LocalArticleNavigator
import com.example.fragmject.core.navigation.contract.LocalAuthNavigator
import com.example.fragmject.core.navigation.contract.LocalCollectionNavigator
import com.example.fragmject.core.navigation.contract.LocalDemoNavigator
import com.example.fragmject.core.navigation.contract.LocalHomeNavigator
import com.example.fragmject.core.navigation.contract.LocalPictureNavigator
import com.example.fragmject.core.navigation.contract.LocalSearchNavigator
import com.example.fragmject.core.navigation.contract.LocalUserNavigator
import com.example.fragmject.core.navigation.runtime.NavFlowScopeContributor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeState: ThemeState

    @Inject
    lateinit var webViewPool: WebViewPool

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    @Inject
    lateinit var navigatorBundle: AppNavigatorBundle

    @Inject
    lateinit var navContributors: Set<@JvmSuppressWildcards NavContentContributor>

    @Inject
    lateinit var flowScopeContributors: Set<@JvmSuppressWildcards NavFlowScopeContributor>

    /** 运行时深层链接请求（onNewIntent 触发），首次启动走 onCreate 的 initialBackStack。 */
    private val pendingDeepLink = mutableStateOf<DeepLinkRequest?>(null)

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
        // 解析冷启动深层链接，得到初始返回栈（热启动深层链接走 onNewIntent → pendingDeepLink）
        val initialBackStack = resolveDeepLinkBackStack(intent)
        setContent {
            val darkTheme by themeState.darkTheme
                .collectAsStateWithLifecycle(initialValue = false)
            AppTheme(window = window, darkTheme = darkTheme) {
                rememberWindowSizeClass {
                    CompositionLocalProvider(
                        LocalArticleNavigator provides navigatorBundle.articleNavigator,
                        LocalUserNavigator provides navigatorBundle.userNavigator,
                        LocalAuthNavigator provides navigatorBundle.authNavigator,
                        LocalCollectionNavigator provides navigatorBundle.collectionNavigator,
                        LocalSearchNavigator provides navigatorBundle.searchNavigator,
                        LocalDemoNavigator provides navigatorBundle.demoNavigator,
                        LocalPictureNavigator provides navigatorBundle.pictureNavigator,
                        LocalHomeNavigator provides navigatorBundle.homeNavigator,
                    ) {
                        AppNavGraph(
                            navigationDispatcher = navigationDispatcher,
                            navContributors = navContributors,
                            flowScopeContributors = flowScopeContributors,
                            initialBackStack = initialBackStack,
                            pendingDeepLink = pendingDeepLink.value,
                            onDeepLinkConsumed = { pendingDeepLink.value = null },
                        )
                    }
                }
            }
        }
        // WebView 预创建（内部已在主线程 IdleHandler 中执行，不阻塞首帧）
        webViewPool.prepare(applicationContext)
        // 仅在 Debug 构建中启用 WebView 调试，避免在 Release 包暴露调试接口
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = intent.data?.let { DeepLinkRequest(uri = it) }
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