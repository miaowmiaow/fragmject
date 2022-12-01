package com.example.fragment.module.user.fragment

import android.net.Uri
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.compose.PullRefreshLazyColumn
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.theme.WanTheme
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.model.MyCollectViewModel

class MyCollectFragment : RouterFragment() {

    private val viewModel: MyCollectViewModel by viewModels()

    private var refreshing by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    private val _articleResult = ArrayList<ArticleBean>().toMutableStateList()
    private val articleResult get() = _articleResult

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    MyCollectPage()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearMyCollectArticleResult()
    }

    override fun initView() {
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.myCollectArticleResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) { bean ->
                bean.data?.datas?.let { data ->
                    data.forEach { item -> item.collect = true }
                    if (viewModel.isHomePage()) {
                        _articleResult.clear()
                    }
                    _articleResult.addAll(data)
                }
            }
            //设置下拉刷新状态
            refreshing = false
            //设置加载更多状态
            loading = viewModel.hasNextPage()
        }
        return viewModel
    }

    @Composable
    fun MyCollectPage() {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.theme))
                    .systemBarsPadding()
            ) {
                IconButton(
                    onClick = { onBackPressed() }, modifier = Modifier.height(45.dp)
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
            PullRefreshLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                items = articleResult,
                refreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    viewModel.getMyCollectArticleHome()
                },
                loading = loading,
                onLoad = {
                    loading = false
                    viewModel.getMyCollectArticleNext()
                }
            ) { index, item ->
                ArticleCard(
                    index,
                    item,
                    onClick = {
                        navigation(Router.WEB, bundleOf(Keys.URL to Uri.encode(item.link)))
                    },
                    avatarClick = {
                        navigation(Router.SHARE_ARTICLE, bundleOf(Keys.UID to item.userId))
                    },
                    tagClick = {
                        val uriString = "https://www.wanandroid.com${item.tags?.get(0)?.url}"
                        val uri = Uri.parse(uriString)
                        var cid = uri.getQueryParameter(Keys.CID)
                        if (cid.isNullOrBlank()) {
                            val paths = uri.pathSegments
                            if (paths != null && paths.size >= 3) {
                                cid = paths[2]
                            }
                        }
                        navigation(Router.SYSTEM, bundleOf(Keys.CID to cid))
                    },
                    chapterNameClick = {
                        navigation(Router.SYSTEM, bundleOf(Keys.CID to item.chapterId))
                    }
                )
            }
        }

    }
}