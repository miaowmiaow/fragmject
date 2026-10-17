package com.example.fragmject.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import com.example.fragmject.core.designsystem.LocalWindowSizeClass
import com.example.fragmject.core.designsystem.NAV_TRANSITION_DURATION_MS
import com.example.fragmject.core.domain.AuthStatus
import com.example.fragmject.core.navigation.contract.AuthFlowNavKey
import com.example.fragmject.core.navigation.contract.DetailPaneNavKey
import com.example.fragmject.core.navigation.contract.FlowScopedNavKey
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.contract.RequiresAuth
import com.example.fragmject.core.navigation.runtime.LocalDetailContent
import com.example.fragmject.core.navigation.runtime.LocalOnClearDetail
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.navigation.runtime.LocalSelectedDetailKey
import com.example.fragmject.core.navigation.runtime.NavContentContributor
import com.example.fragmject.core.navigation.runtime.NavContentRegistry
import com.example.fragmject.core.navigation.runtime.ProvideDetailPaneViewModelStore
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.home.MainNavKey

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
    onDeepLinkConsumed: () -> Unit = {},
    /** 栈内仅剩首页时的返回出口（交由 Activity 结束/退到后台）。 */
    onExit: () -> Unit = {},
) {
    val navViewModel: AppNavViewModel = viewModel()
    val authStatus by navViewModel.authStatus.collectAsStateWithLifecycle()
    // 初始栈中「需登录」「详情面板」两类目标不直接入栈：前者交深链守卫经 navigate 判定，
    // 后者需等首帧确定 isExpanded 后再经 placeKey 分流（否则大屏会被当成全屏页渲染）。
    val backStack = rememberNavBackStack(
        *initialBackStack
            .filterNot { it is RequiresAuth || it is DetailPaneNavKey }
            .ifEmpty { listOf(MainNavKey) }
            .toTypedArray()
    )

    // ---- Expanded 列表-详情同屏状态 ----
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    // 详情面板选中态：复用 Navigation 3 的 NavKey 反射序列化（与 rememberNavBackStack 同源），
    // 使进程重建后与 backStack 一起恢复，避免「栈恢复了但面板空白」的不一致。
    val selectedDetailKeys = rememberSerializable(
        serializer = NavBackStackSerializer<NavKey>(NavKeySerializer()),
    ) { NavBackStack<NavKey>() }

    // ---- 选图结果清理 ----
    // 图片流程全部退出时清空结果：系统返回键不走选择器的取消回调，
    // 否则发起方下次进入仍会看到上一次选中的图片。
    val pictureNavigator = LocalNavigators.current.picture
    val hasPictureFlow = backStack.any { it is FlowScopedNavKey }
    // 进入图片流程时清空上一次的选图结果。
    // 结果只在「确认选择」时写入（PictureNavRegistration.onConfirm），而系统返回/手势返回
    // 不会走选择器的取消回调。因此在**进入**时清空比在退出时清空更可靠：退出时清空会把
    // 刚确认的结果一并擦掉（曾表现为「选完图回到发起方却不显示」）。
    LaunchedEffect(hasPictureFlow) {
        if (hasPictureFlow) pictureNavigator.clearSelection()
    }

    // ---- 导航动作（直接操作 backStack） ----
    // 关键：NavDisplay 按 NavKey 缓存 entry 内容，MainNavKey 不变时 MainScreen
    // 不会被重组，因此 navigate/placeKey 等 lambda 必须保持稳定引用，内部通过
    // rememberUpdatedState 读取最新的登录态，避免闭包捕获过期状态。
    val currentAuthStatus by rememberUpdatedState(authStatus)
    val currentIsExpanded by rememberUpdatedState(isExpanded)

    // 把 key 落到最终位置（详情面板或全屏栈顶），不涉及登录守卫。
    // navigate 走守卫后调用；onAuthSuccess / 深链以「绕过守卫」的方式直接调用。
    val placeKey: (NavKey) -> Unit = remember {
        { key ->
            if (currentIsExpanded && isDetailPaneKey(key)) {
                selectedDetailKeys.clear()
                selectedDetailKeys.add(key)
            } else {
                backStack.add(key)
            }
        }
    }

    val navigate: (NavKey) -> Unit = remember {
        { key ->
            if (key is RequiresAuth && currentAuthStatus == AuthStatus.UNKNOWN) {
                navViewModel.deferredNavigationState.value = key
            } else if (requiredLoginNavKey(key, currentAuthStatus)) {
                navViewModel.setPendingRedirect(key)
                backStack.add(LoginNavKey)
            } else if (backStack.lastOrNull() == key) {
                // 去重：与栈顶相同的 key 不再入栈，避免「返回一次仍停在同一页」
            } else {
                placeKey(key)
            }
        }
    }
    // onExit 来自 Activity，经 rememberUpdatedState 读取最新引用，避免 remember 捕获过期值
    val currentOnExit by rememberUpdatedState(onExit)
    val navigateUp: () -> Unit = remember {
        {
            if (backStack.size > 1) {
                val popped = backStack.removeLastOrNull()
                // 离开认证流程时放弃待回跳目标，避免下次登录成功后被迫跳转
                if (popped is AuthFlowNavKey) {
                    navViewModel.clearPendingRedirect()
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

    // ---- 登录态确定后处理暂存的受保护目标 ----
    LaunchedEffect(authStatus) {
        if (authStatus != AuthStatus.UNKNOWN) {
            navViewModel.deferredNavigationState.value?.let { key ->
                navViewModel.deferredNavigationState.value = null
                navigate(key)
            }
        }
    }

    // ---- 深层链接守卫 ----
    // 受保护目标一律经 navigate：未登录时跳转登录并在成功后回跳，登录态未知时先暂存。
    // 冷启动：初始栈已剔除受保护目标与详情面板目标，这里只执行一次（rememberSaveable 覆盖
    // 配置变更与进程重建）。
    var initialDeepLinkGuarded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialDeepLinkGuarded) {
            initialDeepLinkGuarded = true
            initialBackStack.dropWhile { it == MainNavKey }.forEach { key ->
                when {
                    key is RequiresAuth -> navigate(key)
                    key is DetailPaneNavKey -> placeKey(key)
                    // 全屏且无需登录的目标已直接进初始栈，此处无需处理
                }
            }
        }
    }
    // 运行时深层链接（onNewIntent 触发）：首次启动由初始栈处理，此处仅处理后续动态深层链接。
    LaunchedEffect(pendingDeepLink) {
        val request = pendingDeepLink ?: return@LaunchedEffect
        matchDeepLink(request)?.let { newStack ->
            // 保留栈底 Main，避免用户在多层流程中时收到深链导致在途流程被整体丢弃
            backStack.clear()
            backStack.add(MainNavKey)
            newStack.dropWhile { it == MainNavKey }.forEach { key ->
                if (key is RequiresAuth) navigate(key) else placeKey(key)
            }
        }
        onDeepLinkConsumed()
    }

    // ---- 绑定导航能力到语义 Navigator ----
    // navigate/navigateUp/popBackStack 均为 remember 稳定引用，内部状态经
    // rememberUpdatedState 与闭包捕获的稳定 State 读取，故只需绑定一次。
    DisposableEffect(navigationDispatcher) {
        val releaseBinding = navigationDispatcher.bind(
            navigate = navigate,
            navigateUp = navigateUp,
            popBackStack = popBackStack,
            onAuthSuccess = {
                val target = navViewModel.consumePendingRedirect()
                // 连续弹出栈顶的认证页（Login/Register），避免从登录页进注册页回跳后残留
                while (backStack.lastOrNull() is AuthFlowNavKey) {
                    backStack.removeLastOrNull()
                }
                if (target != null) {
                    // 登录成功时登录态尚未经 StateFlow→Compose 多跳异步更新，直接 placeKey
                    // 绕过 navigate 的守卫判断，避免被 requiredLoginNavKey 再次拦截形成死循环。
                    placeKey(target)
                } else {
                    // 无回跳目标：清栈回首页
                    popBackStack(MainNavKey)
                }
            },
        )
        onDispose { releaseBinding() }
    }

    // ---- 统一导航内容注册表 ----
    // 全屏 entry 与面板 detailContent 共用同一份「NavKey → 渲染器」映射，
    // 回调作为运行时参数注入：全屏走 navigateUp，面板走 clearDetail。
    val registry = remember(navContributors) {
        NavContentRegistry().apply {
            navContributors.forEach { it.contribute(this) }
        }
    }
    val detailContent = remember(registry) {
        val renderer: @Composable (NavKey) -> Unit = { key ->
            // DetailPane 不在 NavDisplay 内，拿不到 entry 级 ViewModelStore；
            // 这里按 key 提供一个等价作用域，使两栏模式与单栏行为一致。
            ProvideDetailPaneViewModelStore(key = key) {
                CompositionLocalProvider(
                    LocalOnNavigateUp provides { selectedDetailKeys.clear() },
                ) {
                    registry.Render(key)
                }
            }
        }
        renderer
    }

    CompositionLocalProvider(
        LocalSelectedDetailKey provides selectedDetailKeys.lastOrNull(),
        LocalOnClearDetail provides { selectedDetailKeys.clear() },
        LocalDetailContent provides detailContent,
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
private fun requiredLoginNavKey(key: NavKey, status: AuthStatus): Boolean {
    return key is RequiresAuth && status == AuthStatus.LOGGED_OUT
}

/**
 * 判定指定路由在 Expanded 模式下是否应由右侧 DetailPane 渲染（而非全屏 backStack 推入）。
 */
private fun isDetailPaneKey(key: NavKey): Boolean = key is DetailPaneNavKey
