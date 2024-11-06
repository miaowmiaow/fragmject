package com.example.fragment.project.ui.web

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.launch

/**
 * 导航图
 */
@Composable
fun WebViewNavGraph(
    url: String,
    modifier: Modifier = Modifier,
    navigator: WebViewNavigator,
    navigateUp: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
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
                url = backStackEntry.arguments?.getString("url") ?: url,
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
                onReceivedTitle = { url, title ->
                    scope.launch {
                        WanHelper.setBrowseHistory(title.toString(), url.toString())
                    }
                },
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