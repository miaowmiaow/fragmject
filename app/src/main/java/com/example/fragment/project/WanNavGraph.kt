package com.example.fragment.project

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fragment.project.database.user.User
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
import com.example.miaow.base.vm.TRANSITION_TIME

/**
 * 导航图
 */
@Composable
fun WanNavGraph(
    route: String?,
    modifier: Modifier = Modifier,
    viewModel: WanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val wanNavActions =
        remember(navController, uiState.user) { WanNavActions(navController, uiState.user) }
    NavHost(
        navController = navController,
        startDestination = WanDestinations.MAIN_ROUTE,
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
        composable(WanDestinations.BROWSE_HISTORY_ROUTE) {
            BrowseHistoryScreen(
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.DEMO_ROUTE) {
            DemoScreen(onNavigateUp = { wanNavActions.navigateUp() })
        }
        composable(WanDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onNavigateToRegister = { wanNavActions.navigateToRegister() },
                onNavigateUp = { wanNavActions.navigateUp() },
                onPopBackStackToMain = { wanNavActions.popBackStack(WanDestinations.MAIN_ROUTE) }
            )
        }
        composable(WanDestinations.MAIN_ROUTE) {
            MainScreen(
                hotKeyData = uiState.hotKeyResult,
                systemData = uiState.treeResult,
                onNavigateToBookmarkHistory = { wanNavActions.navigateToBookmarkHistory() },
                onNavigateToDemo = { wanNavActions.navigateToDemo() },
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToMyCoin = { wanNavActions.navigateToMyCoin() },
                onNavigateToMyCollect = { wanNavActions.navigateToMyCollect() },
                onNavigateToMyShare = { wanNavActions.navigateToMyShare() },
                onNavigateToSearch = { wanNavActions.navigateToSearch(it) },
                onNavigateToSetting = { wanNavActions.navigateToSetting() },
                onNavigateToShareArticle = { wanNavActions.navigateToShareArticle() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) }
            )
        }
        composable(WanDestinations.MY_COIN_ROUTE) {
            MyCoinScreen(
                onNavigateToRank = { wanNavActions.navigateToRank() },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.MY_COLLECT_ROUTE) {
            MyCollectScreen(
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.MY_SHARE_ROUTE) {
            MyShareScreen(
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.RANK_ROUTE) {
            RankScreen(
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.REGISTER_ROUTE) {
            RegisterScreen(
                onNavigateUp = { wanNavActions.navigateUp() },
                onPopBackStackToMain = { wanNavActions.popBackStack(WanDestinations.MAIN_ROUTE) }
            )
        }
        composable("${WanDestinations.SEARCH_ROUTE}/{key}") { backStackEntry ->
            SearchScreen(
                key = backStackEntry.arguments?.getString("key") ?: "",
                hotKeyData = uiState.hotKeyResult,
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.SETTING_ROUTE) {
            SettingScreen(
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.SHARE_ARTICLE_ROUTE) {
            ShareArticleScreen(
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable("${WanDestinations.SYSTEM_ROUTE}/{cid}") { backStackEntry ->
            val cid = backStackEntry.arguments?.getString("cid").toString()
            uiState.treeResult.forEach { data ->
                data.children?.forEachIndexed { index, children ->
                    if (children.id == cid) {
                        SystemScreen(
                            title = data.name,
                            tabIndex = index,
                            systemData = data.children,
                            onNavigateToLogin = { wanNavActions.navigateToLogin() },
                            onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                            onNavigateToUser = { wanNavActions.navigateToUser(it) },
                            onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                            onNavigateUp = { wanNavActions.navigateUp() }
                        )
                        return@composable
                    }
                }
            }
        }
        composable("${WanDestinations.USER_ROUTE}/{userId}") { backStackEntry ->
            UserScreen(
                userId = backStackEntry.arguments?.getString("userId") ?: "",
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable("${WanDestinations.WEB_ROUTE}/{url}") { backStackEntry ->
            WebScreen(
                url = backStackEntry.arguments?.getString("url") ?: "",
                onNavigateToBookmarkHistory = { wanNavActions.navigateToBookmarkHistory() },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
    }
    if (!route.isNullOrBlank()) {
        val webRoute = "${WanDestinations.WEB_ROUTE}/"
        wanNavActions.navigate(
            if (route.startsWith(webRoute)) {
                webRoute + Uri.encode(route.substring(webRoute.length))
            } else {
                route
            }
        )
    }
}

class WanNavActions(
    private val navController: NavHostController,
    private val user: User?,
) {
    val navigateToBookmarkHistory: () -> Unit = {
        navigate(WanDestinations.BROWSE_HISTORY_ROUTE)
    }
    val navigateToDemo: () -> Unit = {
        navigate(WanDestinations.DEMO_ROUTE)
    }
    val navigateToLogin: () -> Unit = {
        navigate(WanDestinations.LOGIN_ROUTE)
    }
    val navigateToMyCoin: () -> Unit = {
        navigate(WanDestinations.MY_COIN_ROUTE)
    }
    val navigateToMyCollect: () -> Unit = {
        navigate(WanDestinations.MY_COLLECT_ROUTE)
    }
    val navigateToMyShare: () -> Unit = {
        navigate(WanDestinations.MY_SHARE_ROUTE)
    }
    val navigateToRank: () -> Unit = {
        navigate(WanDestinations.RANK_ROUTE)
    }
    val navigateToRegister: () -> Unit = {
        navigate(WanDestinations.REGISTER_ROUTE)
    }
    val navigateToSearch: (key: String) -> Unit = {
        navigate(WanDestinations.SEARCH_ROUTE + "/$it")
    }
    val navigateToSetting: () -> Unit = {
        navigate(WanDestinations.SETTING_ROUTE)
    }
    val navigateToShareArticle: () -> Unit = {
        navigate(WanDestinations.SHARE_ARTICLE_ROUTE)
    }
    val navigateToSystem: (cid: String) -> Unit = {
        navigate(WanDestinations.SYSTEM_ROUTE + "/$it")
    }
    val navigateToUser: (userId: String) -> Unit = {
        navigate(WanDestinations.USER_ROUTE + "/$it")
    }
    val navigateToWeb: (url: String) -> Unit = {
        navigate(WanDestinations.WEB_ROUTE + "/${Uri.encode(it)}")
    }
    val navigateUp: () -> Unit = {
        if (!navController.navigateUp()) {
            navigate(WanDestinations.MAIN_ROUTE)
        }
    }
    val popBackStack: (route: String) -> Unit = {
        navController.popBackStack(it, false)
    }

    fun navigate(route: String) {
        navController.graph.findNode(route) ?: return
        if (requiredLoginRoute(route, user)) {
            navController.navigate(WanDestinations.LOGIN_ROUTE)
        } else {
            navController.navigate(route)
        }
    }
}

object WanDestinations {
    const val BROWSE_HISTORY_ROUTE = "browse_history_route"
    const val DEMO_ROUTE = "demo_route"
    const val LOGIN_ROUTE = "login_route"
    const val MAIN_ROUTE = "main_route"
    const val MY_COIN_ROUTE = "my_coin_route"
    const val MY_COLLECT_ROUTE = "my_collect_route"
    const val MY_SHARE_ROUTE = "my_share_route"
    const val RANK_ROUTE = "rank_route"
    const val REGISTER_ROUTE = "register_route"
    const val SEARCH_ROUTE = "search_route"
    const val SETTING_ROUTE = "setting_route"
    const val SHARE_ARTICLE_ROUTE = "share_article_route"
    const val SYSTEM_ROUTE = "system_route"
    const val USER_ROUTE = "user_route"
    const val WEB_ROUTE = "web_route"
}

private fun requiredLoginRoute(route: String, user: User?): Boolean {
    return (route.startsWith(WanDestinations.MY_COIN_ROUTE)
            || route.startsWith(WanDestinations.MY_COLLECT_ROUTE)
            || route.startsWith(WanDestinations.MY_SHARE_ROUTE))
            && (user == null || user.id.isBlank())
}