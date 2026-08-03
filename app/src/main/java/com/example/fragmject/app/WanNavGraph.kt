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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fragmject.core.common.RequiresAuth
import com.example.fragmject.core.database.store.UserStore
import com.example.fragmject.core.database.model.UserEntity
import com.example.fragmject.feature.picture.PictureEditorNavKey
import com.example.fragmject.feature.picture.PicturePreviewNavKey
import com.example.fragmject.feature.picture.PictureSelectorNavKey
import com.example.fragmject.feature.picture.ui.editor.PictureEditorScreen
import com.example.fragmject.feature.picture.ui.selector.PicturePreviewScreen
import com.example.fragmject.feature.picture.ui.selector.PictureSelectorScreen
import com.example.fragmject.feature.picture.ui.selector.PictureViewModel
import com.example.fragmject.feature.picture.ui.selector.PreviewMode
import com.example.fragmject.feature.wan.BrowseHistoryNavKey
import com.example.fragmject.feature.wan.DemoNavKey
import com.example.fragmject.feature.wan.LoginNavKey
import com.example.fragmject.feature.wan.MainNavKey
import com.example.fragmject.feature.wan.MyCoinNavKey
import com.example.fragmject.feature.wan.MyCollectNavKey
import com.example.fragmject.feature.wan.MyShareNavKey
import com.example.fragmject.feature.wan.RankNavKey
import com.example.fragmject.feature.wan.RegisterNavKey
import com.example.fragmject.feature.wan.SearchNavKey
import com.example.fragmject.feature.wan.SettingNavKey
import com.example.fragmject.feature.wan.ShareArticleNavKey
import com.example.fragmject.feature.wan.SystemNavKey
import com.example.fragmject.feature.wan.UserNavKey
import com.example.fragmject.feature.wan.WebNavKey
import com.example.fragmject.feature.wan.browse_history.BrowseHistoryScreen
import com.example.fragmject.feature.wan.demo.DemoScreen
import com.example.fragmject.feature.wan.login.LoginScreen
import com.example.fragmject.feature.wan.main.MainScreen
import com.example.fragmject.feature.wan.my_coin.MyCoinScreen
import com.example.fragmject.feature.wan.my_collect.MyCollectScreen
import com.example.fragmject.feature.wan.my_share.MyShareScreen
import com.example.fragmject.feature.wan.rank.RankScreen
import com.example.fragmject.feature.wan.register.RegisterScreen
import com.example.fragmject.feature.wan.search.SearchScreen
import com.example.fragmject.feature.wan.setting.SettingScreen
import com.example.fragmject.feature.wan.share.ShareArticleScreen
import com.example.fragmject.feature.wan.system.SystemScreen
import com.example.fragmject.feature.wan.user.UserScreen
import com.example.fragmject.feature.wan.web.WebScreen
import com.example.fragmject.core.common.TransitionGuard

/**
 * 导航图。
 *
 * 迁移到 Navigation 3：back stack 由 [rememberNavBackStack] 创建，导航动作通过直接操作
 * [NavBackStack]（MutableList）完成，不再依赖 NavController；路由通过 [entryProvider] DSL
 * 映射到 Composable，参数直接通过类型化 key 获取，无需 toRoute()。
 */
@Composable
fun WanNavGraph(
    modifier: Modifier = Modifier
) {
    val user by UserStore.getUser().collectAsStateWithLifecycle(initialValue = null)
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
    val popBackStack: (NavKey) -> Unit = remember {{
        val targetClass = it::class
        val index = backStack.indexOfLast { entry -> entry::class == targetClass }
        if (index >= 0) {
            repeat(backStack.size - index - 1) { backStack.removeLastOrNull() }
        }
    }}

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        transitionSpec = {
            slideInHorizontally(tween(TransitionGuard.DURATION_MS.toInt())) { it } togetherWith
                    slideOutHorizontally(tween(TransitionGuard.DURATION_MS.toInt())) { -it }
        },
        popTransitionSpec = {
            slideInHorizontally(tween(TransitionGuard.DURATION_MS.toInt())) { -it } togetherWith
                    slideOutHorizontally(tween(TransitionGuard.DURATION_MS.toInt())) { it }
        },
        entryProvider = entryProvider {
            entry<BrowseHistoryNavKey> {
                BrowseHistoryScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<DemoNavKey> {
                DemoScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp
                )
            }
            entry<LoginNavKey> {
                LoginScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                    onPopBackStack = popBackStack,
                )
            }
            entry<MainNavKey> {
                MainScreen(
                    onNavigate = navigate,
                    selectedDetailKey = selectedDetailKey,
                    onClearDetail = { selectedDetailKey = null },
                )
            }
            entry<MyCoinNavKey> {
                MyCoinScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<MyCollectNavKey> {
                MyCollectScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<MyShareNavKey> {
                MyShareScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<RankNavKey> {
                RankScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<RegisterNavKey> {
                RegisterScreen(
                    onNavigateUp = navigateUp,
                    onPopBackStack = popBackStack,
                )
            }
            entry<SearchNavKey> { route ->
                SearchScreen(
                    key = route.key,
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<SettingNavKey> {
                SettingScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<ShareArticleNavKey> {
                ShareArticleScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<SystemNavKey> { route ->
                SystemScreen(
                    cid = route.cid,
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<UserNavKey> { route ->
                UserScreen(
                    userId = route.userId,
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<WebNavKey> { route ->
                WebScreen(
                    url = route.url,
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            // ── Picture ──
            entry<PictureSelectorNavKey> {
                PictureSelectorScreen(
                    onFinish = { navigateUp() },
                    onDismiss = { navigateUp() },
                    onPreview = { positions ->
                        navigate(PicturePreviewNavKey(positions))
                    },
                    viewModel = pictureViewModel,
                )
            }
            entry<PicturePreviewNavKey> { route ->
                PicturePreviewScreen(
                    mode = PreviewMode.NORM,
                    origSelectPosition = route.positions,
                    previewPosition = 0,
                    onFinish = { navigateUp() },
                    onDismiss = { navigateUp() },
                    onOpenEditor = { uri ->
                        navigate(PictureEditorNavKey(uri.toString()))
                    },
                    viewModel = pictureViewModel,
                )
            }
            entry<PictureEditorNavKey> { route ->
                val oldUri = route.oldUriString.toUri()
                PictureEditorScreen(
                    bitmapUri = oldUri,
                    onFinish = { _, _ -> navigateUp() },
                    onDismiss = { navigateUp() },
                )
            }
        }
    )
}

/**
 * 深层链接待支持，详情参考 WanNavGraph:
 * wan://com.fragment.project/rank
 * wan://com.fragment.project/search/$key
 * wan://com.fragment.project/web/${Uri.encode(url)}
 * 示例代码如下：
 * val deepLinkIntent = Intent(
 *     Intent.ACTION_VIEW,
 *     "wan://com.fragment.project/web/${Uri.encode("http://www.baidu.com")}".toUri(),
 * )
 * val deepLinkPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
 *     addNextIntentWithParentStack(deepLinkIntent)
 *     getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 * }
 * deepLinkPendingIntent?.send()
 */
// const val fragmentUri = "wan://com.fragment.project"

// ---- 辅助函数 ----

/**
 * 判定指定路由是否需要登录态。依赖 [RequiresAuth] 标记接口自动识别。
 */
private fun requiredLoginNavKey(key: NavKey, user: UserEntity?): Boolean {
    return key is RequiresAuth && (user == null || user.id <= 0)
}

/**
 * 判定指定路由在 Expanded 模式下是否应由右侧 DetailPane 渲染（而非全屏 backStack 推入）。
 */
private fun isDetailPaneKey(key: NavKey): Boolean {
    return key is WebNavKey || key is UserNavKey || key is SystemNavKey ||
        key is SettingNavKey || key is MyCoinNavKey || key is MyCollectNavKey ||
        key is MyShareNavKey || key is RankNavKey || key is BrowseHistoryNavKey
}