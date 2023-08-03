package com.example.fragment.project.ui.main.home

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.ArticleListBean
import com.example.fragment.project.bean.BannerListBean
import com.example.fragment.project.bean.TopArticleBean
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var finishing: Boolean = false,
    var result: MutableList<ArticleBean> = ArrayList(),
)

class HomeViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeState())

    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(refreshing = true, loading = false, finishing = false)
        }
        viewModelScope.launch {
            //通过async获取首页需要展示的数据
            val banner = async { getBanner() }
            val articleTop = async { getArticleTop() }
            val articleList = async { getArticleList(getHomePage()) }
            val articleData: MutableList<ArticleBean> = arrayListOf()
            banner.await().data?.let { articleData.add(ArticleBean(banners = it, viewType = 0)) }
            articleTop.await().data?.onEach { it.top = true }?.let { articleData.addAll(it) }
            articleList.await().data?.datas?.let { articleData.addAll(it) }
            _uiState.update {
                it.copy(
                    refreshing = false,
                    loading = hasNextPage(),
                    finishing = !hasNextPage(),
                    result = articleData
                )
            }
        }
    }

    fun getNext() {
        _uiState.update {
            it.copy(refreshing = false, loading = false, finishing = false)
        }
        viewModelScope.launch {
            val response = getArticleList(getNextPage())
            _uiState.update { state ->
                response.data?.datas?.let { datas ->
                    state.result.addAll(datas)
                }
                state.copy(refreshing = false, loading = hasNextPage(), finishing = !hasNextPage())
            }
        }
    }

    /**
     * 获取banner
     */
    private suspend fun getBanner(): BannerListBean {
        return coroutineScope { get(HttpRequest("banner/json")) }
    }

    /**
     * 获取置顶文章
     */
    private suspend fun getArticleTop(): TopArticleBean {
        return coroutineScope { get(HttpRequest("article/top/json")) }
    }

    /**
     * 获取首页文章列表
     * page 0开始
     */
    private suspend fun getArticleList(page: Int): ArticleListBean {
        val request = HttpRequest("article/list/{page}/json").putPath("page", page.toString())
        val response = coroutineScope { get<ArticleListBean>(request) }
        updatePageCont(response.data?.pageCount?.toInt())
        return response
    }

}