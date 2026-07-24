package com.example.fragment.project

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fragment.project.data.User
import com.example.fragment.project.ui.browse_history.BrowseHistoryScreen
import com.example.fragment.project.ui.demo.DemoScreen
import com.example.fragment.project.ui.login.LoginScreen
import com.example.fragment.project.ui.main.MainScreen
import com.example.fragment.project.ui.my_coin.MyCoinScreen
import com.example.fragment.project.ui.my_collect.MyCollectScreen
import com.example.fragment.project.ui.my_share.MyShareScreen
import com.example.fragment.project.ui.rank.RankScreen
import com.example.fragment.project.ui.register.RegisterScreen
import com.example.fragment.project.ui.search.SearchScreen
import com.example.fragment.project.ui.setting.SettingScreen
import com.example.fragment.project.ui.share.ShareArticleScreen
import com.example.fragment.project.ui.system.SystemScreen
import com.example.fragment.project.ui.user.UserScreen
import com.example.fragment.project.ui.web.WebScreen
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.TRANSITION_TIME
import kotlinx.serialization.Serializable

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
    val user by WanHelper.getUser().collectAsStateWithLifecycle(initialValue = null)
    val backStack = rememberNavBackStack(MainNavKey)

    // ---- 导航动作（直接操作 backStack） ----
    val navigate: (NavKey) -> Unit = remember(user) {{
        if (requiredLoginNavKey(it, user)) {
            backStack.add(LoginNavKey)
        } else {
            backStack.add(it)
        }
    }}
    val navigateUp: () -> Unit = remember {
        { if (backStack.size > 1) backStack.removeLastOrNull() else backStack.add(MainNavKey) }
    }
    val popBackStack: (NavKey) -> Unit = remember {{
        val targetClass = it::class
        val index = backStack.indexOfLast { entry -> entry::class == targetClass }
        if (index >= 0) {
            repeat(backStack.size - index) { backStack.removeLastOrNull() }
        }
    }}

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        transitionSpec = {
            slideInHorizontally(tween(TRANSITION_TIME)) { it } togetherWith
                    slideOutHorizontally(tween(TRANSITION_TIME)) { -it }
        },
        popTransitionSpec = {
            slideInHorizontally(tween(TRANSITION_TIME)) { -it } togetherWith
                    slideOutHorizontally(tween(TRANSITION_TIME)) { it }
        },
        entryProvider = entryProvider {
            entry<BrowseHistoryNavKey> {
                BrowseHistoryScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                )
            }
            entry<DemoNavKey> {
                DemoScreen(onNavigateUp = navigateUp)
            }
            entry<LoginNavKey> {
                LoginScreen(
                    onNavigate = navigate,
                    onNavigateUp = navigateUp,
                    onPopBackStack = popBackStack,
                )
            }
            entry<MainNavKey> {
                MainScreen(onNavigate = navigate)
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
const val fragmentUri = "wan://com.fragment.project"

// ---- 路由定义 —— 所有路由实现 NavKey ----

@Serializable
object BrowseHistoryNavKey : RequiresAuth

@Serializable
object DemoNavKey : NavKey

@Serializable
object LoginNavKey : NavKey

@Serializable
object MainNavKey : NavKey

@Serializable
object MyCoinNavKey : RequiresAuth

@Serializable
object MyCollectNavKey : RequiresAuth

@Serializable
object MyShareNavKey : RequiresAuth

@Serializable
object RankNavKey : NavKey

@Serializable
object RegisterNavKey : NavKey

@Serializable
data class SearchNavKey(val key: String) : NavKey

@Serializable
object SettingNavKey : NavKey

@Serializable
object ShareArticleNavKey : RequiresAuth

@Serializable
data class SystemNavKey(val cid: String) : NavKey

@Serializable
data class UserNavKey(val userId: String) : NavKey

@Serializable
data class WebNavKey(val url: String) : NavKey

// ---- 接口定义 ----

/**
 * 标记接口：实现此接口的路由需要登录态才能访问，同时继承 [NavKey] 以兼容 Navigation 3。
 * 新增需登录页面时，只需让路由对象/数据类实现本接口即可。
 */
interface RequiresAuth : NavKey

/**
 * 判定指定路由是否需要登录态。依赖 [RequiresAuth] 标记接口自动识别。
 */
private fun requiredLoginNavKey(key: NavKey, user: User?): Boolean {
    return key is RequiresAuth && (user == null || user.id <= 0)
}