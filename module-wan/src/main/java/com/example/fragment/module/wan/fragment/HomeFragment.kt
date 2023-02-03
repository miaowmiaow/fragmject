package com.example.fragment.module.wan.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fragment.library.base.compose.PullRefreshLayout
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.R
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.model.HomeViewModel
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

class HomeFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    HomeScreen()
                }
            }
        }
    }

    override fun initView() {}

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    @Composable
    fun HomeScreen() {
        val viewModel: HomeViewModel = viewModel()
        PullRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            refreshing = viewModel.refreshing,
            onRefresh = {
                viewModel.getArticleHome()
            },
            loading = viewModel.loading,
            onLoad = {
                viewModel.getArticleNext()
            },
            onNoData = {
                viewModel.getArticleHome()
            },
            items = viewModel.result,
        ) { _, item ->
            if (item.viewType == 0) {
                BannerCard(item)
            } else {
                ArticleCard(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    item = item,
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
                    },
                    onSignIn = {
                        navigation(Router.USER_LOGIN)
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun BannerCard(item: ArticleBean) {
        item.banners?.let {
            val pageCount = it.size
            val startIndex = Int.MAX_VALUE / 2
            val pagerState = rememberPagerState(initialPage = startIndex)
            val coroutineScope = rememberCoroutineScope()
            DisposableEffect(Unit) {
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }, 3000, 3000)
                onDispose {
                    timer.cancel()
                }
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                HorizontalPager(
                    count = Int.MAX_VALUE,
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 32.dp),
                ) { page ->
                    Card(
                        Modifier
                            .graphicsLayer {
                                val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue
                                lerp(
                                    start = 0.9f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                ).also { scale ->
                                    scaleX = scale
                                    scaleY = scale
                                }
                            }
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        val index = (page - startIndex).floorMod(pageCount)
                        if (it[index].imagePath.isNotBlank()) {
                            AsyncImage(
                                model = it[index].imagePath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16f))
                                    .clickable {
                                        navigation(
                                            Router.WEB,
                                            bundleOf(Keys.URL to Uri.encode(it[index].url))
                                        )
                                    }
                            )
                        }
                    }
                }
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.paddingFromBaseline(bottom = 15.dp),
                    pageCount = pageCount,
                    pageIndexMapping = { page ->
                        (page - startIndex).floorMod(pageCount)
                    },
                    activeColor = colorResource(R.color.orange),
                    inactiveColor = colorResource(R.color.theme)
                )
            }
        }
    }

    private fun Int.floorMod(other: Int): Int = when (other) {
        0 -> this
        else -> this - floorDiv(other) * other
    }

}