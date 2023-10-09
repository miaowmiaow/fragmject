package com.example.fragment.project.ui.web.content

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
    webViewNavigator: WebViewNavigator,
    modifier: Modifier = Modifier,
    onWebBrowseHistory: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateUp: () -> Unit = {},
) {
    val webViewNavController = rememberNavController()
    val webViewNavActions = remember(webViewNavController) {
        WebViewNavActions(webViewNavController)
    }
    NavHost(
        navController = webViewNavController,
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
                navigator = webViewNavigator,
                onBack = {
                    webViewNavActions.navigateUp()
                },
                onForward = {
                    webViewNavActions.navigateToWebView("about:blank")
                },
                shouldOverrideUrl = {
                    onWebBrowseHistory(true, it)
                    webViewNavActions.navigateToWebView(it)
                },
                onNavigateUp = onNavigateUp
            )
        }
    }
}

class WebViewNavActions(
    private val navController: NavHostController
) {
    val navigateToWebView: (url: String) -> Unit = {
        navController.navigate(
            WebViewDestinations.WEB_VIEW_ROUTE + "/${Uri.encode(it)}",
            navOptions { launchSingleTop = false }
        )
    }
    val navigateUp: () -> Unit = {
        navController.navigateUp()
    }
}

object WebViewDestinations {
    const val WEB_VIEW_ROUTE = "web_view_route"
}