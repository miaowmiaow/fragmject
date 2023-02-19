package com.example.fragment.project

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.fragment.project.ui.login.LoginScreen
import com.example.fragment.project.ui.main.MainScreen
import com.example.fragment.project.ui.main.user.UserViewModel
import com.example.fragment.project.ui.register.RegisterScreen
import com.example.fragment.project.ui.system.SystemScreen
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
) {
    val statusBarColor = colorResource(R.color.theme)
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(LocalLifecycleOwner.current) {
        systemUiController.setStatusBarColor(
            statusBarColor,
            darkIcons = false
        )
    }
    val userViewModel: UserViewModel = viewModel()
    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val wanNavActions = remember(navController, userUiState) {
        WanNavActions(navController, userUiState.getUserId())
    }
    val wanViewModel: WanViewModel = viewModel()
    val wanUiState by wanViewModel.uiState.collectAsStateWithLifecycle()
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        },
    ) {
        composable(WanDestinations.LOGIN_ROUTE) {
            LoginScreen(
                onNavigateToRegister = {
                    wanNavActions.navigateToRegister()
                }
            )
        }
        composable(WanDestinations.MAIN_ROUTE) {
            MainScreen(
                hotKey = wanUiState.hotKeyResult,
                tree = wanUiState.treeResult,
                onNavigateToLogin = {
                    wanNavActions.navigateToLogin()
                },
                onNavigateToSystem = {
                    wanNavActions.navigateToSystem(it)
                },
                onNavigateToWeb = {
                    wanNavActions.navigateToWeb(it)
                }
            )
        }
        composable(WanDestinations.MY_COLLECT) {
        }
        composable(WanDestinations.REGISTER_ROUTE) {
            RegisterScreen()
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
                            onNavigateToLogin = {
                                wanNavActions.navigateToLogin()
                            },
                            onNavigateToSystem = {
                                wanNavActions.navigateToSystem(it)
                            },
                            onNavigateToWeb = {
                                wanNavActions.navigateToWeb(it)
                            }
                        )
                        return@composable
                    }
                }
            }
        }
        composable("${WanDestinations.WEB_ROUTE}/{url}") { backStackEntry ->
            WebScreen(backStackEntry.arguments?.getString("url") ?: "")
        }
    }
}

private val authentication = arrayOf(
    WanDestinations.SYSTEM_ROUTE
)

class WanNavActions(
    private val navController: NavHostController,
    private val userId: String
) {
    val navigateToLogin: () -> Unit = {
        navigate(WanDestinations.LOGIN_ROUTE)
    }
    val navigateToMyCollect: () -> Unit = {
        navigate(WanDestinations.MY_COLLECT)
    }
    val navigateToRegister: () -> Unit = {
        navigate(WanDestinations.REGISTER_ROUTE)
    }
    val navigateToSystem: (cid: String) -> Unit = {
        navigate(WanDestinations.SYSTEM_ROUTE, "/$it")
    }
    val navigateToWeb: (url: String) -> Unit = {
        navigate(WanDestinations.WEB_ROUTE, "/$it")
    }

    private fun navigate(directions: String, arguments: String = "") {
        if (authentication.contains(directions) && userId.isBlank()) {
            navController.navigate(WanDestinations.LOGIN_ROUTE)
        } else {
            navController.navigate(directions + arguments)
        }
    }
}

object WanDestinations {
    const val LOGIN_ROUTE = "login"
    const val MY_COLLECT = "my_collect"
    const val MAIN_ROUTE = "main"
    const val REGISTER_ROUTE = "register"
    const val SYSTEM_ROUTE = "system"
    const val WEB_ROUTE = "web"
}