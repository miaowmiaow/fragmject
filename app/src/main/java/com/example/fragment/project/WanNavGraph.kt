package com.example.fragment.project

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.fragment.project.ui.login.LoginScreen
import com.example.fragment.project.ui.main.MainScreen
import com.example.fragment.project.ui.my_collect.MyCollectScreen
import com.example.fragment.project.ui.my_share.MyShareScreen
import com.example.fragment.project.ui.register.RegisterScreen
import com.example.fragment.project.ui.system.SystemScreen
import com.example.fragment.project.ui.web.WebScreen
import com.example.fragment.project.utils.WanHelper
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
    val context = LocalContext.current
    val statusBarColor = colorResource(R.color.theme)
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(LocalLifecycleOwner.current) {
        systemUiController.setStatusBarColor(
            statusBarColor,
            darkIcons = false
        )
    }
    val wanNavActions = remember(navController) {
        WanNavActions(navController)
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
                },
                onPopBackStackToMain = {
                    wanNavActions.popBackStackToMain()
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
                onNavigateToMyCoin = {
                    if (context is AppCompatActivity) {
                        Toast.makeText(context, "正在重构中...", Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToMyCollect = {
                    wanNavActions.navigateToMyCollect()
                },
                onNavigateToMyShare = {
                    wanNavActions.navigateToMyShare()
                },
                onNavigateToSearch = {
                    if (context is AppCompatActivity) {
                        Toast.makeText(context, "正在重构中...", Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToShareArticle = {
                    if (context is AppCompatActivity) {
                        Toast.makeText(context, "正在重构中...", Toast.LENGTH_SHORT).show()
                    }
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
            MyCollectScreen(
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
        composable(WanDestinations.MY_SHARE) {
            MyShareScreen(
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
    WanDestinations.MY_COLLECT,
    WanDestinations.MY_SHARE,
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
    val navigateToMyCollect: () -> Unit = {
        navigate(WanDestinations.MY_COLLECT)
    }
    val navigateToMyShare: () -> Unit = {
        navigate(WanDestinations.MY_SHARE)
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
        WanHelper.getUser { userBean ->
            if (authentication.contains(directions) && userBean.id.isBlank()) {
                navController.navigate(WanDestinations.LOGIN_ROUTE)
            } else {
                navController.navigate(directions + arguments)
            }
        }
    }
}

object WanDestinations {
    const val LOGIN_ROUTE = "login"
    const val MAIN_ROUTE = "main"
    const val MY_COLLECT = "my_collect"
    const val MY_SHARE = "my_share"
    const val REGISTER_ROUTE = "register"
    const val SYSTEM_ROUTE = "system"
    const val WEB_ROUTE = "web"
}