package com.example.fragment.project

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.fragment.project.ui.login.LoginScreen
import com.example.fragment.project.ui.main.MainScreen
import com.example.fragment.project.ui.register.RegisterScreen
import com.example.fragment.project.ui.system.SystemScreen
import com.example.fragment.project.ui.system.SystemTreeViewModel
import com.example.fragment.project.ui.system.SystemViewModel
import com.example.fragment.project.ui.web.WebScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WanNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberAnimatedNavController(),
    startDestination: String = WanDestinations.MAIN_ROUTE,
    systemTreeViewModel: SystemTreeViewModel = viewModel(),
    systemViewModel: SystemViewModel = viewModel(),
) {
    val statusBarColor = colorResource(R.color.theme)
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(LocalLifecycleOwner.current) {
        systemUiController.setStatusBarColor(
            statusBarColor,
            darkIcons = false
        )
    }
    val navigationActions = remember(navController) {
        WanNavActions(navController)
    }
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        },
    ) {
        composable(
            WanDestinations.LOGIN_ROUTE,
        ) {
            LoginScreen(
                onNavigateToRegister = {
                    navigationActions.navigateToRegister()
                }
            )
        }
        composable(WanDestinations.MAIN_ROUTE) {
            MainScreen(
                onNavigateToLogin = {
                    navigationActions.navigateToLogin()
                },
                onNavigateToSystem = {
                    navigationActions.navigateToSystem(it)
                },
                onNavigateToWeb = {
                    navigationActions.navigateToWeb(it)
                }
            )
        }
        composable(WanDestinations.MY_COIN_ROUTE) {}
        composable(WanDestinations.MY_COLLECT_ROUTE) {}
        composable(WanDestinations.MY_INFO_ROUTE) {}
        composable(WanDestinations.MY_SHARE_ROUTE) {}
        composable(WanDestinations.REGISTER_ROUTE) {
            RegisterScreen()
        }
        composable(WanDestinations.SHARE_ARTICLE_ROUTE) {}
        composable(WanDestinations.SETTING_ROUTE) {}
        composable("${WanDestinations.SYSTEM_ROUTE}/{cid}") { backStackEntry ->
            SystemScreen(
                backStackEntry.arguments?.getString("cid").toString(),
                systemTreeViewModel,
                systemViewModel
            )
        }
        composable(WanDestinations.USER_INFO_ROUTE) {}
        composable("${WanDestinations.WEB_ROUTE}/{url}") { backStackEntry ->
            WebScreen(backStackEntry.arguments?.getString("url") ?: "")
        }
    }
}

class WanNavActions(private val navController: NavHostController) {
    val navigateToLogin: () -> Unit = {
        navController.navigate(WanDestinations.LOGIN_ROUTE)
    }
    val navigateToMain: () -> Unit = {
        navController.navigate(WanDestinations.MAIN_ROUTE)
    }
    val navigateToMyCoin: () -> Unit = {
        navController.navigate(WanDestinations.MY_COIN_ROUTE)
    }
    val navigateToMyCollect: () -> Unit = {
        navController.navigate(WanDestinations.MY_COLLECT_ROUTE)
    }
    val navigateToMyInfo: () -> Unit = {
        navController.navigate(WanDestinations.MY_INFO_ROUTE)
    }
    val navigateToMyShare: () -> Unit = {
        navController.navigate(WanDestinations.MY_SHARE_ROUTE)
    }
    val navigateToRegister: () -> Unit = {
        navController.navigate(WanDestinations.REGISTER_ROUTE)
    }
    val navigateToSearch: () -> Unit = {
        navController.navigate(WanDestinations.SEARCH_ROUTE)
    }
    val navigateToSetting: () -> Unit = {
        navController.navigate(WanDestinations.SETTING_ROUTE)
    }
    val navigateToShareArticle: () -> Unit = {
        navController.navigate(WanDestinations.SHARE_ARTICLE_ROUTE)
    }
    val navigateToSystem: (cid: String) -> Unit = {
        navController.navigate("${WanDestinations.SYSTEM_ROUTE}/$it")
    }
    val navigateToWeb: (url: String) -> Unit = {
        navController.navigate("${WanDestinations.WEB_ROUTE}/$it")
    }
}

object WanDestinations {
    const val LOGIN_ROUTE = "login"
    const val MAIN_ROUTE = "main"
    const val MY_COIN_ROUTE = "my_coin"
    const val MY_COLLECT_ROUTE = "my_collect"
    const val MY_INFO_ROUTE = "my_info"
    const val MY_SHARE_ROUTE = "my_share"
    const val REGISTER_ROUTE = "register"
    const val SEARCH_ROUTE = "search"
    const val SETTING_ROUTE = "setting"
    const val SHARE_ARTICLE_ROUTE = "share_article"
    const val SYSTEM_ROUTE = "system"
    const val USER_INFO_ROUTE = "user_info"
    const val WEB_ROUTE = "web"
}