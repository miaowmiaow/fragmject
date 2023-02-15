package com.example.fragment.module.wan.vm

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.BannerListBean
import com.example.fragment.library.common.bean.TopArticleBean
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
            it.copy(refreshing = true)
        }
        //通过viewModelScope创建一个协程
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
                it.copy(refreshing = false, loading = hasNextPage(), result = articleData)
            }
        }
    }

    fun getNext() {
        _uiState.update {
            it.copy(loading = false)
        }
        viewModelScope.launch {
            _uiState.update {
                getArticleList(getNextPage()).data?.datas?.let { datas ->
                    it.result.addAll(datas)
                }
                it.copy(refreshing = false, loading = hasNextPage())
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
        //构建请求体，传入请求参数
        val request = HttpRequest("article/list/{page}/json").putPath("page", page.toString())
        //以get方式发起网络请求
        val response = coroutineScope { get<ArticleListBean>(request) { updateProgress(it) } }
        //根据接口返回更新总页码
        response.data?.pageCount?.let { updatePageCont(it.toInt()) }
        return response
    }

}