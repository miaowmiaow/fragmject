package com.example.fragment.project

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.example.fragment.project.ui.browse_collect.BrowseCollectScreen
import com.example.fragment.project.ui.login.LoginScreen
import com.example.fragment.project.ui.main.MainScreen
import com.example.fragment.project.ui.my_coin.MyCoinScreen
import com.example.fragment.project.ui.my_collect.MyCollectScreen
import com.example.fragment.project.ui.my_demo.MyDemoScreen
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * 导航图
 */
@Composable
fun WanNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val wanNavActions = remember(navController) {
        WanNavActions(navController)
    }
    val viewModel: WanViewModel = viewModel()
    val wanUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusBarColor = colorResource(R.color.theme)
    val systemUiController = rememberSystemUiController()
    DisposableEffect(lifecycleOwner) {
        systemUiController.setStatusBarColor(
            statusBarColor,
            darkIcons = false
        )
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onSaveWanHelper()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.background(colorResource(R.color.background)),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        },
    ) {
        composable(WanDestinations.BROWSE_COLLECT_ROUTE) {
            BrowseCollectScreen(
                webBrowseList = wanUiState.webBrowseResult,
                webCollectList = wanUiState.webCollectResult,
                onWebBrowse = { isAdd, text -> viewModel.onWebBrowse(isAdd, text) },
                onWebCollect = { isAdd, text -> viewModel.onWebCollect(isAdd, text) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
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
                hotKeyList = wanUiState.hotKeyResult,
                treeList = wanUiState.treeResult,
                onNavigateToBrowseCollect = { wanNavActions.navigateToBrowseCollect() },
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToMyCoin = { wanNavActions.navigateToMyCoin() },
                onNavigateToMyCollect = { wanNavActions.navigateToMyCollect() },
                onNavigateToMyDemo = { wanNavActions.navigateToMyDemo() },
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
                onNavigateToCoinRank = { wanNavActions.navigateToRank() },
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
        composable(WanDestinations.MY_DEMO_ROUTE) {
            MyDemoScreen()
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
                hotKeyList = wanUiState.hotKeyResult,
                searchHistoryList = wanUiState.searchHistoryResult,
                onSearchHistory = { isAdd, text -> viewModel.onSearchHistory(isAdd, text) },
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
        composable(WanDestinations.SETTING_ROUTE) {
            SettingScreen(
                onNavigateToAbout = { wanNavActions.navigateToWeb("https://wanandroid.com") },
                onNavigateToFeedback = { wanNavActions.navigateToWeb("https://github.com/miaowmiaow/fragmject/issues") },
                onNavigateToPrivacyPolicy = { wanNavActions.navigateToWeb("file:///android_asset/privacy_policy.html") },
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
            wanUiState.treeResult.forEach { data ->
                data.children?.forEachIndexed { index, children ->
                    if (children.id == cid) {
                        SystemScreen(
                            title = data.name,
                            index = index,
                            tree = data.children,
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
                originalUrl = backStackEntry.arguments?.getString("url") ?: "",
                webCollectList = wanUiState.webCollectResult,
                onWebBrowse = { isAdd, text -> viewModel.onWebBrowse(isAdd, text) },
                onWebCollect = { isAdd, text -> viewModel.onWebCollect(isAdd, text) },
                onNavigateUp = { wanNavActions.navigateUp() }
            )
        }
    }
}

private val authentication = arrayOf(
    WanDestinations.MY_COIN_ROUTE,
    WanDestinations.MY_COLLECT_ROUTE,
    WanDestinations.MY_SHARE_ROUTE,
)

class WanNavActions(
    private val navController: NavHostController
) {
    val navigateToLogin: () -> Unit = {
        navigate(WanDestinations.LOGIN_ROUTE)
    }
    val navigateToBrowseCollect: () -> Unit = {
        navigate(WanDestinations.BROWSE_COLLECT_ROUTE)
    }
    val navigateToMyCoin: () -> Unit = {
        navigate(WanDestinations.MY_COIN_ROUTE)
    }
    val navigateToMyCollect: () -> Unit = {
        navigate(WanDestinations.MY_COLLECT_ROUTE)
    }
    val navigateToMyDemo: () -> Unit = {
        navigate(WanDestinations.MY_DEMO_ROUTE)
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
        navigate(WanDestinations.SEARCH_ROUTE, "/$it")
    }
    val navigateToSetting: () -> Unit = {
        navigate(WanDestinations.SETTING_ROUTE)
    }
    val navigateToShareArticle: () -> Unit = {
        navigate(WanDestinations.SHARE_ARTICLE_ROUTE)
    }
    val navigateToSystem: (cid: String) -> Unit = {
        navigate(WanDestinations.SYSTEM_ROUTE, "/$it")
    }
    val navigateToUser: (userId: String) -> Unit = {
        navigate(WanDestinations.USER_ROUTE, "/$it")
    }
    val navigateToWeb: (url: String) -> Unit = {
        navigate(WanDestinations.WEB_ROUTE, "/${Uri.encode(it)}")
    }
    val navigateUp: () -> Unit = {
        navController.navigateUp()
    }
    val popBackStack: (route: String) -> Unit = {
        navController.popBackStack(it, false)
    }

    private fun navigate(directions: String, arguments: String = "") {
        val options = navOptions { launchSingleTop = true }
        WanHelper.getUser { userBean ->
            if (authentication.contains(directions) && userBean.id.isBlank()) {
                navController.navigate(WanDestinations.LOGIN_ROUTE, options)
            } else {
                navController.navigate(directions + arguments, options)
            }
        }
    }
}

object WanDestinations {
    const val LOGIN_ROUTE = "login"
    const val MAIN_ROUTE = "main"
    const val BROWSE_COLLECT_ROUTE = "BROWSE_COLLECT"
    const val MY_COIN_ROUTE = "my_coin"
    const val MY_COLLECT_ROUTE = "my_collect"
    const val MY_DEMO_ROUTE = "my_demo"
    const val MY_SHARE_ROUTE = "my_share"
    const val RANK_ROUTE = "rank"
    const val REGISTER_ROUTE = "register"
    const val SEARCH_ROUTE = "search"
    const val SETTING_ROUTE = "setting"
    const val SHARE_ARTICLE_ROUTE = "share_article"
    const val SYSTEM_ROUTE = "system"
    const val USER_ROUTE = "user"
    const val WEB_ROUTE = "web"
}