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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.example.fragment.project.ui.login.LoginScreen
import com.example.fragment.project.ui.main.MainScreen
import com.example.fragment.project.ui.my_browse.MyBrowseScreen
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
) {
    val statusBarColor = colorResource(R.color.theme)
    val systemUiController = rememberSystemUiController()
    DisposableEffect(LocalLifecycleOwner.current) {
        systemUiController.setStatusBarColor(
            statusBarColor,
            darkIcons = false
        )
        onDispose { }
    }
    val wanNavActions = remember(navController) {
        WanNavActions(navController)
    }
    val wanViewModel: WanViewModel = viewModel()
    val wanUiState by wanViewModel.uiState.collectAsStateWithLifecycle()
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
        composable(WanDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onNavigateToRegister = { wanNavActions.navigateToRegister() },
                onPopBackStackToMain = { wanNavActions.popBackStackToMain() }
            )
        }
        composable(WanDestinations.MAIN_ROUTE) {
            MainScreen(
                hotKey = wanUiState.hotKeyResult,
                tree = wanUiState.treeResult,
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToBrowse = { wanNavActions.navigateToMyBrowse() },
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
        composable(WanDestinations.MY_BROWSE_ROUTE) {
            MyBrowseScreen(onNavigateToWeb = { wanNavActions.navigateToWeb(it) })
        }
        composable(WanDestinations.MY_COIN_ROUTE) {
            MyCoinScreen(onNavigateToCoinRank = { wanNavActions.navigateToRank() })
        }
        composable(WanDestinations.MY_COLLECT_ROUTE) {
            MyCollectScreen(
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) }
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
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) }
            )
        }
        composable(WanDestinations.RANK_ROUTE) {
            RankScreen(onNavigateToWeb = { wanNavActions.navigateToWeb(it) })
        }
        composable(WanDestinations.REGISTER_ROUTE) {
            RegisterScreen()
        }
        composable("${WanDestinations.SEARCH_ROUTE}/{key}") { backStackEntry ->
            SearchScreen(
                hotKey = wanUiState.hotKeyResult,
                key = backStackEntry.arguments?.getString("key") ?: "",
                onNavigateToLogin = { wanNavActions.navigateToLogin() },
                onNavigateToSystem = { wanNavActions.navigateToSystem(it) },
                onNavigateToUser = { wanNavActions.navigateToUser(it) },
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) }
            )
        }
        composable(WanDestinations.SETTING_ROUTE) {
            SettingScreen(
                onNavigateToPrivacyPolicy = { wanNavActions.navigateToWeb("file:///android_asset/privacy_policy.html") },
                onNavigateToFeedback = { wanNavActions.navigateToWeb("https://github.com/miaowmiaow/fragmject/issues") },
                onNavigateToAbout = { wanNavActions.navigateToWeb("https://wanandroid.com") }
            )
        }
        composable(WanDestinations.SHARE_ARTICLE_ROUTE) {
            ShareArticleScreen(onNavigateToWeb = { wanNavActions.navigateToWeb(it) })
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
                            onNavigateToWeb = { wanNavActions.navigateToWeb(it) }
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
                onNavigateToWeb = { wanNavActions.navigateToWeb(it) }
            )
        }
        composable("${WanDestinations.WEB_ROUTE}/{url}") { backStackEntry ->
            WebScreen(backStackEntry.arguments?.getString("url") ?: "")
        }
    }
}

private val authentication = arrayOf(
    WanDestinations.MY_COIN_ROUTE,
    WanDestinations.MY_COLLECT_ROUTE,
    WanDestinations.MY_SHARE_ROUTE,
    WanDestinations.SHARE_ARTICLE_ROUTE,
)

class WanNavActions(
    private val navController: NavHostController
) {
    val navigateToLogin: () -> Unit = {
        navigate(WanDestinations.LOGIN_ROUTE)
    }
    val popBackStackToMain: () -> Unit = {
        navController.popBackStack(WanDestinations.MAIN_ROUTE, false)
    }
    val navigateToMyBrowse: () -> Unit = {
        navigate(WanDestinations.MY_BROWSE_ROUTE)
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
    const val MY_BROWSE_ROUTE = "my_browse"
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