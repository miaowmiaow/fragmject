package com.example.fragmject.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fragmject.core.navigation.DetailPaneNavKey
import com.example.fragmject.core.navigation.RequiresAuth
import com.example.fragmject.core.model.User
import com.example.fragmject.core.navigation.NavCallbacks
import com.example.fragmject.core.navigation.NavContentRegistry
import com.example.fragmject.feature.picture.ui.selector.PictureViewModel
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.home.MainNavKey
import com.example.fragmject.feature.user.nav.registerUserNavContents
import com.example.fragmject.feature.article.nav.registerArticleNavContents
import com.example.fragmject.feature.collection.nav.registerCollectionNavContents
import com.example.fragmject.feature.auth.nav.registerAuthNavContents
import com.example.fragmject.feature.home.nav.registerHomeNavContents
import com.example.fragmject.feature.demo.nav.registerDemoNavContents
import com.example.fragmject.feature.search.nav.registerSearchNavContents
import com.example.fragmject.feature.picture.nav.registerPictureNavContents

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
    modifier: Modifier = Modifier
) {
    val navViewModel: AppNavViewModel = viewModel()
    val user by navViewModel.user.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(MainNavKey)

    // Picture 模块的共享 ViewModel，跨 Selector/Preview/Editor 三个页面
    val pictureViewModel: PictureViewModel = viewModel()

    // ---- Expanded 列表-详情同屏状态 ----
    // 仅在 Expanded 模式下使用：点击文章时不走 backStack，而是由 MainScreen 右侧面板渲染
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedDetailKey by remember { mutableStateOf<NavKey?>(null) }

    // ---- 导航动作（直接操作 backStack） ----
    // 关键：NavDisplay 按 NavKey 缓存 entry 内容，MainNavKey 不变时 MainScreen
    // 不会被重组，因此 navigate lambda 必须保持稳定引用，内部通过
    // rememberUpdatedState 读取最新的 user，避免闭包捕获过期状态。
    val currentUser by rememberUpdatedState(user)
    val currentIsExpanded by rememberUpdatedState(isExpanded)
    val navigate: (NavKey) -> Unit = remember {
        { key ->
            if (requiredLoginNavKey(key, currentUser)) {
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

    registry.apply {
        registerUserNavContents()
        registerArticleNavContents()
        registerCollectionNavContents()
        registerAuthNavContents()
        registerHomeNavContents(selectedDetailKey, { selectedDetailKey = null }, detailContent)
        registerDemoNavContents()
        registerSearchNavContents()
        registerPictureNavContents(pictureViewModel)
    }

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

/**
 * 深层链接待支持，详情参考 AppNavGraph:
 * fragmject://com.fragment.project/rank
 * fragmject://com.fragment.project/search/$key
 * fragmject://com.fragment.project/web/${Uri.encode(url)}
 * 示例代码如下：
 * val deepLinkIntent = Intent(
 *     Intent.ACTION_VIEW,
 *     "fragmject://com.fragment.project/web/${Uri.encode("http://www.baidu.com")}".toUri(),
 * )
 * val deepLinkPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
 *     addNextIntentWithParentStack(deepLinkIntent)
 *     getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 * }
 * deepLinkPendingIntent?.send()
 */
// const val fragmentUri = "fragmject://com.fragment.project"

// ---- 辅助函数 ----

/**
 * 判定指定路由是否需要登录态。依赖 [RequiresAuth] 标记接口自动识别。
 */
private fun requiredLoginNavKey(key: NavKey, user: User?): Boolean {
    return key is RequiresAuth && (user == null || user.id <= 0)
}

/**
 * 判定指定路由在 Expanded 模式下是否应由右侧 DetailPane 渲染（而非全屏 backStack 推入）。
 */
private fun isDetailPaneKey(key: NavKey): Boolean = key is DetailPaneNavKey