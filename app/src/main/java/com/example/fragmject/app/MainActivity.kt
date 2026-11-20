package com.example.fragmject.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import com.example.fragmject.app.navigation.AppNavGraph
import com.example.fragmject.app.navigation.NavigationDispatcher
import com.example.fragmject.app.navigation.resolveDeepLinkBackStack
import com.example.fragmject.app.ui.pet.PetHost
import com.example.fragmject.app.ui.pet.rememberPetInputHub
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.designsystem.rememberWindowSizeClass
import com.example.fragmject.core.domain.ThemeState
import com.example.fragmject.core.domain.pet.PetSettingsState
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.contract.Navigators
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.webview.WebViewPool
import com.example.security.SecurityGuard
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 安装 splash 并注册**渐隐退出**：鱼在内容出现时淡出，而不是被"啪"地盖掉。
 *
 * 取舍（这里踩过坑，写清楚免得下次又删一遍）：
 * - 之前是 150ms 的 alpha + 缩放，代价有两层：首屏被多盖 150ms；
 *   Macrobenchmark 的 fullyDrawnReported 容易落在 launching 窗口外，
 *   导致整轮被 "didn't capture all metrics" 丢弃。
 * - 现在只做 **alpha**（不做缩放），时长压到 [SPLASH_FADE_MS]：视觉上够"渐隐"，
 *   开销只多一次图层 alpha 动画。
 */
private fun ComponentActivity.installSplashWithFade() {
    // 只在可调试包做渐隐：release 首帧就绪即移除，避免 140ms 的体感延迟，
    // 也避免 Macrobenchmark 的 fullyDrawnReported 落到 launching 窗口之外
    val fadeMs = if (isDebuggable()) SPLASH_FADE_MS else 0L
    installSplashScreen().setOnExitAnimationListener { provider ->
        if (fadeMs <= 0L) {
            provider.remove()
            return@setOnExitAnimationListener
        }
        provider.view.animate()
            .alpha(0f)
            .setDuration(fadeMs)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { provider.remove() }
            .start()
    }
}

/** 可调试包是否可调（`debuggable` 标志，不依赖 BuildConfig 是否开启）。 */
private fun ComponentActivity.isDebuggable(): Boolean =
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

/**
 * 开屏图标的渐隐时长（毫秒），**仅对可调试包生效**；release 恒为 0（首帧就绪立刻移除）。
 *
 * 需要在本机对比启动耗时（Macrobenchmark）时，用 debug 包并把这个值置 0，
 * 或者直接在 release 包上测（它本来就是 0）。
 */
private const val SPLASH_FADE_MS = 140L

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

    @Inject
    lateinit var petSettingsState: PetSettingsState

    /** 运行时深层链接请求（onNewIntent 触发），首次启动走 onCreate 的 initialBackStack。 */
    private val pendingDeepLink = mutableStateOf<DeepLinkRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashWithFade()
        super.onCreate(savedInstanceState)
        // 安全库命中阻断时，进程会在 blockGraceMs（默认 1.2s）后被 native SIGKILL。
        // 这里不再搭建业务 UI（避免 1.2s 内闪现半截首页），改为展示明确的关闭提示，
        // 让用户知道「不是闪退，是被安全策略终止」。
        val securityBlocked = SecurityGuard.isBlocked()
        val blockMessage = SecurityGuard.blockingMessage()
        // 仅从桌面图标（MAIN/LAUNCHER）冷启动时预热首页首屏：深链直接落详情页的冷启动
        // 不会消费首页数据，预热只会白跑一次请求。必须放在 super.onCreate 之后，
        // 因为 Hilt 的 Activity 字段注入在父类 onCreate 内完成。
        if (intent.isLauncherStart() && !securityBlocked) {
            homeRepository.warmUpFirstPage()
        }
        enableEdgeToEdge()
        // 解析冷启动深层链接，得到初始返回栈（热启动深层链接走 onNewIntent → pendingDeepLink）
        val initialBackStack = resolveDeepLinkBackStack(intent)
        setContent {
            val darkTheme by themeState.darkTheme.collectAsStateWithLifecycle(initialValue = false)
            if (securityBlocked) {
                AppTheme(window = window, darkTheme = darkTheme) {
                    SecurityBlockedScreen(blockMessage)
                }
                return@setContent
            }
            // 宠物设置读应用级状态（StateFlow，Eagerly）：Activity 重建后首帧就是真实值，
            // 不会先按「关闭」渲染一帧（鱼不会闪一下再出现）。
            // 注意它只管「开关读得准」：鱼的位置延续由 core:pet 自己的 FishMotionSaver 负责。
            // 冷启动首帧仍可能读到默认「关闭」，那是 Room 异步首发射的已知限制（见 PetSettingsState KDoc）。
            val petSettings by petSettingsState.settings.collectAsStateWithLifecycle()
            AppTheme(window = window, darkTheme = darkTheme) {
                rememberWindowSizeClass {
                    CompositionLocalProvider(LocalNavigators provides navigators) {
                        val petInput = rememberPetInputHub()
                        // 触摸观察与装饰层都封装在 PetHost 里（挂哪一层、为什么，见该文件注释）
                        PetHost(settings = petSettings, input = petInput) {
                            AppNavGraph(
                                navigationDispatcher = navigationDispatcher,
                                navContributors = navContributors,
                                initialBackStack = initialBackStack,
                                pendingDeepLink = pendingDeepLink.value,
                                onDeepLinkConsumed = { pendingDeepLink.value = null },
                                // 栈内仅剩首页时的返回出口：退到后台而非直接销毁，
                                // 保留任务栈以便再次进入时恢复现场
                                onExit = { moveTaskToBack(true) },
                                onRouteChanged = petInput::notifyRouteChanged,
                            )
                        }
                    }
                }
            }
        }
        // WebView 预创建（内部已在主线程 IdleHandler 中执行，不阻塞首帧）
        // 阻断时跳过：进程即将被终止，预创建只会拖慢提示页首帧
        if (!securityBlocked) {
            webViewPool.prepare(applicationContext)
        }
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
 * 安全命中提示页。
 *
 * 为什么不做成二次确认或「继续使用」按钮：命中级别是 CRITICAL，进程会在 1.2s 内被
 * native SIGKILL，给用户操作只会造成「点了没反应」的错觉。这里只陈述原因。
 *
 * 文案取自 [SecurityGuard.blockingMessage]（例：「APK 签名异常，应用即将关闭」）。
 */
@Composable
private fun SecurityBlockedScreen(message: String) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "安全提示", style = MaterialTheme.typography.titleLarge)
            Text(
                text = message,
                modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
