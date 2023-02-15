package com.example.fragment.module.wan.compose

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.Banner
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.module.wan.vm.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current

    var routerActivity: RouterActivity? = null

    if (context is RouterActivity) {
        routerActivity = context
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.refreshing && !uiState.loading) {
        FullScreenLoading()
    } else {
        SwipeRefresh(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            refreshing = uiState.refreshing,
            loading = uiState.loading,
            onRefresh = { viewModel.getHome() },
            onLoad = { viewModel.getNext() },
            onRetry = { viewModel.getHome() },
            data = uiState.result,
        ) { _, item ->
            if (item.viewType == 0) {
                Banner(
                    data = item.banners,
                    pathMapping = { it.imagePath },
                    onClick = { _, banner ->
                        routerActivity?.navigation(
                            Router.WEB,
                            bundleOf(Keys.URL to Uri.encode(banner.url))
                        )
                    }
                )
            } else {
                ArticleCard(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    item = item
                )
            }
        }
    }
}
