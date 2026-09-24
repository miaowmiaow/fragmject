package com.example.fragmject.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import com.example.fragmject.core.designsystem.LocalWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fragmject.core.navigation.DetailPaneNavKey
import com.example.fragmject.core.navigation.RequiresAuth
import com.example.fragmject.core.navigation.LocalDetailContent
import com.example.fragmject.core.navigation.LocalOnClearDetail
import com.example.fragmject.core.navigation.LocalSelectedDetailKey
import com.example.fragmject.core.navigation.NavCallbacks
import com.example.fragmject.core.navigation.NavContentContributor
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.home.MainNavKey

/** 页面转场动画时长 (ms)，与 Compose 过渡动画同步。 */
private const val NAV_TRANSITION_DURATION_MS = 350

/**
 * 导航图。
 *
 * 迁移到 Navigation 3：back stack 由 [rememberNavBackStack] 创建，导航动作通过直接操作
 * [NavBackStack]（MutableList）完成，不再依赖 NavController；路由通过 [entryProvider] DSL
 * 映射到 Composable，参数直接通过类型化 key 获取，无需 toRoute()。
 */
@Composable
fun AppNavGraph(
    navigationDispatcher: NavigationDispatcher,
    navContributors: Set<NavContentContributor>,
    modifier: Modifier = Modifier,
    initialBackStack: List<NavKey> = listOf(MainNavKey),
    pendingDeepLink: DeepLinkRequest? = null,
) {
    val navViewModel: AppNavViewModel = viewModel()
    val isLoggedIn by navViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(*initialBackStack.toTypedArray())

    // ---- 运行时深层链接（onNewIntent 触发） ----
    // 首次启动由 initialBackStack 初始化 backStack；此处仅处理后续动态深层链接。
    LaunchedEffect(pendingDeepLink) {
        val request = pendingDeepLink ?: return@LaunchedEffect
        matchDeepLink(request)?.let { newStack ->
            backStack.clear()
            backStack.addAll(newStack)
        }
    }

    // ---- Expanded 列表-详情同屏状态 ----
    // 仅在 Expanded 模式下使用：点击文章时不走 backStack，而是由 MainScreen 右侧面板渲染
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedDetailKey by remember { mutableStateOf<NavKey?>(null) }

    // ---- 导航动作（直接操作 backStack） ----
    // 关键：NavDisplay 按 NavKey 缓存 entry 内容，MainNavKey 不变时 MainScreen
    // 不会被重组，因此 navigate lambda 必须保持稳定引用，内部通过
    // rememberUpdatedState 读取最新的登录态，避免闭包捕获过期状态。
    val currentIsLoggedIn by rememberUpdatedState(isLoggedIn)
    val currentIsExpanded by rememberUpdatedState(isExpanded)
    val navigate: (NavKey) -> Unit = remember {
        { key ->
            if (requiredLoginNavKey(key, currentIsLoggedIn)) {
                backStack.add(LoginNavKey)
            } else if (currentIsExpanded && isDetailPaneKey(key)) {
                selectedDetailKey = key
            } else {
                backStack.add(key)
            }
        }
    }
    val navigateUp: () -> Unit = remember {
        { if (backStack.size > 1) backStack.removeLastOrNull() else backStack.add(MainNavKey) }
    }
    val popBackStack: (NavKey) -> Unit = remember {
        {
            val targetClass = it::class
            val index = backStack.indexOfLast { entry -> entry::class == targetClass }
            if (index >= 0) {
                repeat(backStack.size - index - 1) { backStack.removeLastOrNull() }
            }
        }
    }

    // ---- 绑定导航能力到语义 Navigator ----
    // navigate/navigateUp/popBackStack 均为 remember 稳定引用，SideEffect 每次重组后
    // 重新绑定即可（重复赋值无害），语义 Navigator 实现据此把语义动作落到具体 NavKey。
    SideEffect {
        navigationDispatcher.navigate = navigate
        navigationDispatcher.navigateUp = navigateUp
        navigationDispatcher.popBackStack = popBackStack
    }

    // ---- 统一导航内容注册表 ----
    // 全屏 entry 与面板 detailContent 共用同一份「NavKey → 渲染器」映射，
    // 回调作为运行时参数注入：全屏走 navigate/navigateUp，面板走 onClearDetail。
    val registry = NavContentRegistry()
    val fullCallbacks = NavCallbacks(
        onNavigate = navigate,
        onNavigateUp = navigateUp,
        onPopBackStack = popBackStack,
    )
    val detailContent: @Composable (NavKey, (NavKey) -> Unit, () -> Unit) -> Unit =
        { key, onNav, onUp ->
            registry.Render(key, NavCallbacks(onNavigate = onNav, onNavigateUp = onUp))
        }

    navContributors.forEach { it.contribute(registry) }

    CompositionLocalProvider(
        LocalSelectedDetailKey provides selectedDetailKey,
        LocalOnClearDetail provides { selectedDetailKey = null },
        LocalDetailContent provides detailContent,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = modifier,
            transitionSpec = {
                slideInHorizontally(tween(NAV_TRANSITION_DURATION_MS)) { it } togetherWith
                        slideOutHorizontally(tween(NAV_TRANSITION_DURATION_MS)) { -it }
            },
            popTransitionSpec = {
                slideInHorizontally(tween(NAV_TRANSITION_DURATION_MS)) { -it } togetherWith
                        slideOutHorizontally(tween(NAV_TRANSITION_DURATION_MS)) { it }
            },
            entryProvider = entryProvider {
                registry.forEach { clazz, renderer ->
                    addEntryProvider(clazz) { key -> renderer(key, fullCallbacks) }
                }
            }
        )
    }
}

/**
 * 判定指定路由是否需要登录态。依赖 [RequiresAuth] 标记接口自动识别。
 */
private fun requiredLoginNavKey(key: NavKey, isLoggedIn: Boolean): Boolean {
    return key is RequiresAuth && !isLoggedIn
}

/**
 * 判定指定路由在 Expanded 模式下是否应由右侧 DetailPane 渲染（而非全屏 backStack 推入）。
 */
private fun isDetailPaneKey(key: NavKey): Boolean = key is DetailPaneNavKey