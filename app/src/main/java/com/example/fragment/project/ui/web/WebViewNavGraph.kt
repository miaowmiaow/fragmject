package com.example.fragment.project.ui.web

import android.net.Uri
import android.view.View
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
    url: String,
    modifier: Modifier = Modifier,
    onCustomView: (view: View?) -> Unit = {},
    onReceivedTitle: (url: String?, title: String?) -> Unit = { _, _ -> },
    navigator: WebViewNavigator,
    navigateUp: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navActions = remember(navController) { WebViewNavActions(navController) }
    NavHost(
        navController = navController,
        startDestination = WebViewDestinations.WEB_VIEW_ROUTE + "/${Uri.encode(url)}",
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
                url = backStackEntry.arguments?.getString("url") ?: "",
                navigator = navigator,
                goBack = {
                    if (navActions.canBack()) {
                        navActions.navigateBack()
                    } else {
                        navigateUp()
                    }
                },
                goForward = {
                    navActions.navigateToWebView(it)
                },
                navigateUp = navigateUp,
                onReceivedTitle = onReceivedTitle,
                onCustomView = onCustomView,
                shouldOverrideUrl = {
                    navActions.navigateToWebView(it)
                },
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
    val navigateToWebView: (url: String) -> Unit = { url ->
        navigate(WebViewDestinations.WEB_VIEW_ROUTE + "/${Uri.encode(url)}")
    }

    private fun navigate(route: String) {
        navController.navigate(route, navOptions { launchSingleTop = false })
    }
}

object WebViewDestinations {
    const val WEB_VIEW_ROUTE = "web_view_route"
}