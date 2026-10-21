package com.example.fragmject.app

import android.content.Intent
import android.os.Bundle
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
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.designsystem.rememberWindowSizeClass
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.app.navigation.AppNavGraph
import com.example.fragmject.app.navigation.NavigationDispatcher
import com.example.fragmject.app.navigation.resolveDeepLinkBackStack
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.contract.Navigators
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
    lateinit var navigators: Navigators

    @Inject
    lateinit var navContributors: Set<@JvmSuppressWildcards NavContentContributor>

    @Inject
    lateinit var homeRepository: HomeRepository

    /** 运行时深层链接请求（onNewIntent 触发），首次启动走 onCreate 的 initialBackStack。 */
    private val pendingDeepLink = mutableStateOf<DeepLinkRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 不注册自定义退出动画：SplashScreen 在首帧绘制完成后立即移除（系统默认行为）。
        // 原先这里是 150ms 的 alpha + 缩放淡出，代价有两层：
        // 1. 首屏内容被 splash 继续覆盖 150ms，用户感知到的启动更慢；
        // 2. Macrobenchmark 的 fullyDrawnReported 更容易落在 launching 窗口之外，
        //    使该轮被 "didn't capture all metrics" 整轮丢弃（实测 5 轮里丢 3~4 轮，
        //    连 timeToInitialDisplay 都拿不到）。
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // 仅从桌面图标（MAIN/LAUNCHER）冷启动时预热首页首屏：深链直接落详情页的冷启动
        // 不会消费首页数据，预热只会白跑一次请求。必须放在 super.onCreate 之后，
        // 因为 Hilt 的 Activity 字段注入在父类 onCreate 内完成。
        if (intent.isLauncherStart()) {
            homeRepository.warmUpFirstPage()
        }
        enableEdgeToEdge()
        // 解析冷启动深层链接，得到初始返回栈（热启动深层链接走 onNewIntent → pendingDeepLink）
        val initialBackStack = resolveDeepLinkBackStack(intent)
        setContent {
            val darkTheme by themeState.darkTheme.collectAsStateWithLifecycle(initialValue = false)
            AppTheme(window = window, darkTheme = darkTheme) {
                rememberWindowSizeClass {
                    CompositionLocalProvider(LocalNavigators provides navigators) {
                        AppNavGraph(
                            navigationDispatcher = navigationDispatcher,
                            navContributors = navContributors,
                            initialBackStack = initialBackStack,
                            pendingDeepLink = pendingDeepLink.value,
                            onDeepLinkConsumed = { pendingDeepLink.value = null },
                            // 栈内仅剩首页时的返回出口：退到后台而非直接销毁，
                            // 保留任务栈以便再次进入时恢复现场
                            onExit = { moveTaskToBack(true) },
                        )
                    }
                }
            }
        }
        // WebView 预创建（内部已在主线程 IdleHandler 中执行，不阻塞首帧）
        webViewPool.prepare(applicationContext)
        // 仅在 Debug 构建中启用 WebView 调试，避免在 Release 包暴露调试接口
        if (BuildConfig.DEBUG) {
            webViewPool.setDebuggingEnabled(true)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = intent.data?.let { DeepLinkRequest(uri = it) }
    }
}

/**
 * 是否为桌面图标启动（action = MAIN 且带 LAUNCHER category）。
 *
 * 深链启动（action = VIEW）不会命中：这类冷启动直接落到详情页，首页首屏数据不会被消费。
 */
private fun Intent.isLauncherStart(): Boolean =
    action == Intent.ACTION_MAIN &&
        categories?.contains(Intent.CATEGORY_LAUNCHER) == true
