package com.example.fragment.project.ui.web

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions

/**
 * 导航图
 */
@Composable
fun WebViewNavGraph(
    originalUrl: String,
    navigator: WebViewNavigator,
    modifier: Modifier = Modifier,
    shouldOverrideUrl: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navActions = remember(navController) { WebViewNavActions(navController) }
    NavHost(
        navController = navController,
        startDestination = WebViewDestinations.WEB_VIEW_ROUTE + "/${Uri.encode(originalUrl)}",
        modifier = modifier,
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
        composable("${WebViewDestinations.WEB_VIEW_ROUTE}/{url}") { backStackEntry ->
            WebView(
                originalUrl = backStackEntry.arguments?.getString("url") ?: originalUrl,
                navigator = navigator,
                goBack = {
                    if (navActions.canBack()) {
                        navActions.navigateBack()
                    } else {
                        onNavigateUp()
                    }
                },
                goForward = {
                    navActions.navigateForward()
                },
                shouldOverrideUrl = {
                    shouldOverrideUrl(it)
                    navActions.navigateToWebView(it)
                },
                onNavigateUp = onNavigateUp
            )
        }
    }
}

class WebViewNavActions(
    private val navController: NavHostController
) {
    val canBack: () -> Boolean = {
        navController.previousBackStackEntry != null
    }
    val navigateBack: () -> Unit = {
        navController.navigateUp()
    }
    val navigateForward: () -> Unit = {
        navController.navigate(
            WebViewDestinations.WEB_VIEW_ROUTE + "/forward",
            navOptions { launchSingleTop = false }
        )
    }
    val navigateToWebView: (url: String) -> Unit = { url ->
        navController.navigate(
            WebViewDestinations.WEB_VIEW_ROUTE + "/${Uri.encode(url)}",
            navOptions { launchSingleTop = false }
        )
    }
}

object WebViewDestinations {
    const val WEB_VIEW_ROUTE = "web_view_route"
}