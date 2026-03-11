package com.example.fragment.project

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
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
 * 导航图
 */
@Composable
fun WanNavGraph(
    modifier: Modifier = Modifier,
) {
    var user by remember { mutableStateOf<User?>(null) }
    LaunchedEffect(Unit) {
        WanHelper.getUser().collect {
            user = it
        }
    }
    val navController = rememberNavController()
    val wanNavActions = WanNavActions(navController, user)
    /**
     * 支持深层链接，详情参考 WanNavGraph:
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
    NavHost(
        navController = navController,
        startDestination = MainRoute,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(TRANSITION_TIME)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(TRANSITION_TIME)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(TRANSITION_TIME)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(TRANSITION_TIME)
            )
        },
    ) {
        composable<BrowseHistoryRoute> {
            BrowseHistoryScreen(
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<DemoRoute> {
            DemoScreen(onNavigateUp = { wanNavActions.navigateUp() })
        }
        composable<LoginRoute> {
            LoginScreen(
                onNavigateToRegister = { wanNavActions.navigate(RegisterRoute) },
                onNavigateUp = { wanNavActions.navigateUp() },
                onPopBackStackToMain = { wanNavActions.popBackStack(MainRoute) }
            )
        }
        composable<MainRoute> {
            MainScreen(
                onNavigateToBookmarkHistory = { wanNavActions.navigate(BrowseHistoryRoute) },
                onNavigateToDemo = { wanNavActions.navigate(DemoRoute) },
                onNavigateToLogin = { wanNavActions.navigate(LoginRoute) },
                onNavigateToMyCoin = { wanNavActions.navigate(MyCoinRoute) },
                onNavigateToMyCollect = { wanNavActions.navigate(MyCollectRoute) },
                onNavigateToMyShare = { wanNavActions.navigate(MyShareRoute) },
                onNavigateToSearch = { wanNavActions.navigate(SearchRoute(it)) },
                onNavigateToSetting = { wanNavActions.navigate(SettingRoute) },
                onNavigateToShareArticle = { wanNavActions.navigate(ShareArticleRoute) },
                onNavigateToSystem = { wanNavActions.navigate(SystemRoute(it)) },
                onNavigateToUser = { wanNavActions.navigate(UserRoute(it)) },
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) }
            )
        }
        composable<MyCoinRoute> {
            MyCoinScreen(
                onNavigateToRank = { wanNavActions.navigate(RankRoute) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<MyCollectRoute> {
            MyCollectScreen(
                onNavigateToLogin = { wanNavActions.navigate(LoginRoute) },
                onNavigateToSystem = { wanNavActions.navigate(SystemRoute(it)) },
                onNavigateToUser = { wanNavActions.navigate(UserRoute(it)) },
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<MyShareRoute> {
            MyShareScreen(
                onNavigateToLogin = { wanNavActions.navigate(LoginRoute) },
                onNavigateToSystem = { wanNavActions.navigate(SystemRoute(it)) },
                onNavigateToUser = { wanNavActions.navigate(UserRoute(it)) },
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<RankRoute>(
            deepLinks = listOf(
                navDeepLink<RankRoute>(
                    basePath = "$fragmentUri/rank",
                )
            )
        ) {
            RankScreen(
                onNavigateToUser = { wanNavActions.navigate(UserRoute(it)) },
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onNavigateUp = { wanNavActions.navigateUp() },
                onPopBackStackToMain = { wanNavActions.popBackStack(MainRoute) }
            )
        }
        composable<SearchRoute>(
            deepLinks = listOf(
                navDeepLink<SearchRoute>(
                    basePath = "$fragmentUri/search",
                )
            )
        ) { backStackEntry ->
            SearchScreen(
                key = backStackEntry.toRoute<SearchRoute>().key,
                onNavigateToLogin = { wanNavActions.navigate(LoginRoute) },
                onNavigateToSystem = { wanNavActions.navigate(SystemRoute(it)) },
                onNavigateToUser = { wanNavActions.navigate(UserRoute(it)) },
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<SettingRoute> {
            SettingScreen(
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<ShareArticleRoute> {
            ShareArticleScreen(
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<SystemRoute> { backStackEntry ->
            SystemScreen(
                cid = backStackEntry.toRoute<SystemRoute>().cid,
                onNavigateToLogin = { wanNavActions.navigate(LoginRoute) },
                onNavigateToSystem = { wanNavActions.navigate(SystemRoute(it)) },
                onNavigateToUser = { wanNavActions.navigate(UserRoute(it)) },
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<UserRoute> { backStackEntry ->
            UserScreen(
                userId = backStackEntry.toRoute<UserRoute>().userId,
                onNavigateToLogin = { wanNavActions.navigate(LoginRoute) },
                onNavigateToSystem = { wanNavActions.navigate(SystemRoute(it)) },
                onNavigateToWeb = { wanNavActions.navigate(WebRoute(it)) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable<WebRoute>(
            deepLinks = listOf(
                navDeepLink<WebRoute>(
                    basePath = "$fragmentUri/web",
                )
            )
        ) { backStackEntry ->
            WebScreen(
                url = backStackEntry.toRoute<WebRoute>().url,
                onNavigateToBookmarkHistory = { wanNavActions.navigate(BrowseHistoryRoute) },
                onNavigateUp = { wanNavActions.navigateUp() },
                shouldOverrideUrl = { wanNavActions.navigate(WebRoute(it)) },
            )
        }
    }
}

const val fragmentUri = "wan://com.fragment.project"

class WanNavActions(
    private val navController: NavHostController,
    private val user: User?,
) {

    fun <T : Any> navigate(route: T) {
        navController.graph.findNode(route) ?: return
        if (requiredLoginRoute(route, user)) {
            navController.navigate(LoginRoute)
        } else {
            navController.navigate(route)
        }
    }

    fun navigateUp() {
        if (!navController.navigateUp()) {
            navigate(MainRoute)
        }
    }

    fun <T : Any> popBackStack(route: T) {
        navController.popBackStack(route, false)
    }
}

@Serializable
object BrowseHistoryRoute

@Serializable
object DemoRoute

@Serializable
object LoginRoute

@Serializable
object MainRoute

@Serializable
object MyCoinRoute

@Serializable
object MyCollectRoute

@Serializable
object MyShareRoute

@Serializable
object RankRoute

@Serializable
object RegisterRoute

@Serializable
data class SearchRoute(val key: String)

@Serializable
object SettingRoute

@Serializable
object ShareArticleRoute

@Serializable
data class SystemRoute(val cid: String)

@Serializable
data class UserRoute(val userId: String)


@Serializable
data class WebRoute(val url: String)

private fun <T : Any> requiredLoginRoute(route: T, user: User?): Boolean {
    return (route is MyCoinRoute
            || route is MyCollectRoute
            || route is MyShareRoute)
            && (user == null || user.id <= 0)
}