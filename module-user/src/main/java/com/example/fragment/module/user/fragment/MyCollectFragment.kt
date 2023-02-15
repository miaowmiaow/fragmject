package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.model.MyCollectViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MyCollectFragment : RouterFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    MyCollectScreen()
                }
            }
        }
    }

    override fun initView() {}

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    @Composable
    fun MyCollectScreen(viewModel: MyCollectViewModel = viewModel()) {
        val statusBarColor = colorResource(R.color.theme)
        val systemUiController = rememberSystemUiController()
        SideEffect {
            systemUiController.setStatusBarColor(
                statusBarColor,
                darkIcons = false
            )
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Column(
            modifier = Modifier.systemBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(colorResource(R.color.theme))
            ) {
                IconButton(
                    modifier = Modifier.height(45.dp),
                    onClick = { onBackPressed() }
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colorResource(R.color.white)
                    )
                }
                Text(
                    text = "我收藏的文章",
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_fff),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            SwipeRefresh(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                refreshing = uiState.refreshing,
                loading = uiState.loading,
                onRefresh = { viewModel.getHome() },
                onLoad = { viewModel.getNext() },
                onRetry = { viewModel.getHome() },
                data = uiState.result,
            ) { _, item ->
                ArticleCard(item = item)
            }
        }

    }
}