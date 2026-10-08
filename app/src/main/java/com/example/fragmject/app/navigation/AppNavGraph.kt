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
import androidx.compose.ui.Modifier
import com.example.fragmject.core.designsystem.LocalWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.NavBackStack
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.fragmject.core.navigation.runtime.DetailPaneNavKey
import com.example.fragmject.core.navigation.runtime.ProvideDetailPaneViewModelStore
import com.example.fragmject.core.navigation.runtime.RequiresAuth
import com.example.fragmject.core.navigation.runtime.LocalDetailContent
import com.example.fragmject.core.navigation.runtime.LocalNavFlowScopes
import com.example.fragmject.core.navigation.runtime.LocalOnClearDetail
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.navigation.runtime.LocalSelectedDetailKey
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.core.navigation.runtime.NavFlowScope
import com.example.fragmject.core.navigation.runtime.NavFlowScopeContributor
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.auth.RegisterNavKey
import com.example.fragmject.feature.home.MainNavKey
import com.example.fragmject.core.navigation.contract.LocalPictureNavigator
import com.example.fragmject.feature.picture.PictureFlowNavKey

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
    flowScopeContributors: Set<NavFlowScopeContributor>,
    modifier: Modifier = Modifier,
    initialBackStack: List<NavKey> = listOf(MainNavKey),
    pendingDeepLink: DeepLinkRequest? = null,
    onDeepLinkConsumed: () -> Unit = {},
    /** 栈内仅剩首页时的返回出口（交由 Activity 结束/退到后台）。 */
    onExit: () -> Unit = {},
) {
    val navViewModel: AppNavViewModel = viewModel()
    val isLoggedIn by navViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(*initialBackStack.toTypedArray())

    // ---- 运行时深层链接（onNewIntent 触发） ----
    // 首次启动由 initialBackStack 初始化 backStack；此处仅处理后续动态深层链接。
    LaunchedEffect(pendingDeepLink) {
        val request = pendingDeepLink ?: return@LaunchedEffect
        matchDeepLink(request)?.let { newStack ->
            // 保留栈底 Main，避免用户在多层流程中时收到深链导致在途流程被整体丢弃
            backStack.clear()
            backStack.add(MainNavKey)
            backStack.addAll(newStack.dropWhile { it == MainNavKey })
        }
        onDeepLinkConsumed()
    }

    // ---- Expanded 列表-详情同屏状态 ----
    // 仅在 Expanded 模式下使用：点击文章时不走 backStack，而是由 MainScreen 右侧面板渲染
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedDetailKey by remember { mutableStateOf<NavKey?>(null) }

    // ---- 被登录守卫拦截的待回跳目标 ----
    // 守卫拦截未登录访问受保护路由时，在此暂存原目标；登录/注册成功后经
    // onAuthSuccess 消费并回跳。属导航图运行时状态，随 backStack 同生命周期
    // （配置变更重建、进程死亡丢弃，与 backStack 非序列化行为一致）。
    var pendingRedirect by remember { mutableStateOf<NavKey?>(null) }

    // ---- 流程作用域 ----
    // 通用机制：对每个 NavFlowScopeContributor，若 backStack 中存在其流程的 key 则创建
    // 作用域、全部退出时 close 销毁。取代进程级 @Singleton，使在途工作随流程结束取消。
    // app 组合根只依赖通用 NavFlowScopeContributor，不感知具体 feature 流程类型。
    val activeContributors = flowScopeContributors.filter { contributor ->
        backStack.any { contributor.matches(it) }
    }
    // 组合期同步创建作用域（remember 保证首帧即就绪，不再走空态兜底），
    // 已有作用域经 previousScopes 增量保留，退出者由 LaunchedEffect close。
    var previousScopes by remember {
        mutableStateOf<Map<NavFlowScopeContributor, NavFlowScope>>(emptyMap())
    }
    val flowScopes = remember(activeContributors) {
        activeContributors.associateWith { contributor ->
            previousScopes[contributor] ?: contributor.create()
        }
    }
    LaunchedEffect(activeContributors) {
        val toClose = previousScopes.keys.filter { it !in activeContributors }
        toClose.forEach { contributor -> previousScopes[contributor]?.close() }
        previousScopes = flowScopes
    }

    // ---- 选图结果清理 ----
    // 图片流程全部退出时清空结果：系统返回键不走选择器的取消回调，
    // 否则发起方下次进入仍会看到上一次选中的图片。
    val pictureNavigator = LocalPictureNavigator.current
    val hasPictureFlow = backStack.any { it is PictureFlowNavKey }
    // 进入图片流程时清空上一次的选图结果。
    // 结果只在「确认选择」时写入（PictureNavRegistration.onConfirm），而系统返回/手势返回
    // 不会走选择器的取消回调。因此在**进入**时清空比在退出时清空更可靠：退出时清空会把
    // 刚确认的结果一并擦掉（曾表现为「选完图回到发起方却不显示」）。
    LaunchedEffect(hasPictureFlow) {
        if (hasPictureFlow) pictureNavigator.clearSelection()
    }

    // ---- 导航动作（直接操作 backStack） ----
    // 关键：NavDisplay 按 NavKey 缓存 entry 内容，MainNavKey 不变时 MainScreen
    // 不会被重组，因此 navigate lambda 必须保持稳定引用，内部通过
    // rememberUpdatedState 读取最新的登录态，避免闭包捕获过期状态。
    val currentIsLoggedIn by rememberUpdatedState(isLoggedIn)
    val currentIsExpanded by rememberUpdatedState(isExpanded)
    val navigate: (NavKey) -> Unit = remember {
        { key ->
            if (requiredLoginNavKey(key, currentIsLoggedIn)) {
                pendingRedirect = key
                backStack.add(LoginNavKey)
            } else if (backStack.lastOrNull() == key) {
                // 去重：与栈顶相同的 key 不再入栈，避免「返回一次仍停在同一页」
            } else if (currentIsExpanded && isDetailPaneKey(key)) {
                selectedDetailKey = key
            } else {
                backStack.add(key)
            }
        }
    }
    // onExit 来自 Activity，经 rememberUpdatedState 读取最新引用，避免 remember 捕获过期值
    val currentOnExit by rememberUpdatedState(onExit)
    val navigateUp: () -> Unit = remember {
        {
            if (backStack.size > 1) {
                val popped = backStack.removeLastOrNull()
                // 离开登录流程时放弃待回跳目标，避免下次登录成功后被迫跳转
                if (popped is LoginNavKey || popped is RegisterNavKey) {
                    pendingRedirect = null
                }
            } else {
                // 栈内仅剩首页：交由 Activity 退出，此前是空操作导致首页无法用返回键退出
                currentOnExit()
            }
        }
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
    // navigate/navigateUp/popBackStack 均为 remember 稳定引用，内部状态经
    // rememberUpdatedState 与闭包捕获的稳定 State 读取，故只需绑定一次。
    LaunchedEffect(Unit) {
        navigationDispatcher.bind(
            navigate = navigate,
            navigateUp = navigateUp,
            popBackStack = popBackStack,
            onAuthSuccess = {
                val target = pendingRedirect
                pendingRedirect = null
                // 连续弹出栈顶的认证页（Login/Register），避免从登录页进注册页回跳后残留
                while (backStack.lastOrNull() is LoginNavKey || backStack.lastOrNull() is RegisterNavKey) {
                    backStack.removeLastOrNull()
                }
                if (target != null) {
                    // 显式按 isDetailPaneKey 分流，绕过 navigate 的守卫判断：
                    // 登录成功时 isLoggedIn 尚未经 StateFlow→Compose 多跳异步更新，
                    // 若走 navigate 会被 requiredLoginNavKey 再次拦截并把 target 塞回
                    // pendingRedirect，形成回跳死循环。这里直接复用 Expanded 分发逻辑。
                    if (currentIsExpanded && isDetailPaneKey(target)) {
                        selectedDetailKey = target
                    } else {
                        backStack.add(target)
                    }
                } else {
                    // 无回跳目标：清栈回首页
                    popBackStack(MainNavKey)
                }
            },
        )
    }

    // ---- 统一导航内容注册表 ----
    // 全屏 entry 与面板 detailContent 共用同一份「NavKey → 渲染器」映射，
    // 回调作为运行时参数注入：全屏走 navigate/navigateUp，面板走 onClearDetail。
    val registry = remember(navContributors) {
        NavContentRegistry().apply {
            navContributors.forEach { it.contribute(this) }
        }
    }
    val detailContent: @Composable (NavKey) -> Unit =
        { key ->
            // DetailPane 不在 NavDisplay 内，拿不到 entry 级 ViewModelStore；
            // 这里按 key 提供一个等价作用域，使两栏模式与单栏行为一致。
            ProvideDetailPaneViewModelStore(key = key) {
                CompositionLocalProvider(
                    LocalOnNavigateUp provides { selectedDetailKey = null },
                ) {
                    registry.Render(key)
                }
            }
        }

    CompositionLocalProvider(
        LocalSelectedDetailKey provides selectedDetailKey,
        LocalOnClearDetail provides { selectedDetailKey = null },
        LocalDetailContent provides detailContent,
        LocalNavFlowScopes provides flowScopes,
    ) {
        NavDisplay(
            // entry 级作用域：ViewModel 随 NavEntry 出栈而清除（覆盖默认值时必须补回
            // rememberSaveableStateHolderNavEntryDecorator，否则 rememberSaveable 会退回 Activity 级）
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            backStack = backStack,
            // 复用 navigateUp：系统返回键与顶部返回按钮走同一套清理（含登录回跳清理与根栈兜底）
            onBack = navigateUp,
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
                    addEntryProvider(clazz) { key ->
                        CompositionLocalProvider(
                            LocalOnNavigateUp provides navigateUp,
                        ) {
                            renderer(key)
                        }
                    }
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