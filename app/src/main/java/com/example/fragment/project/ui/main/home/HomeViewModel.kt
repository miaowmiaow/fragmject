package com.example.fragment.project.ui.main.home

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.ArticleList
import com.example.fragment.project.data.BannerList
import com.example.fragment.project.data.TopArticle
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var finishing: Boolean = false,
    var result: MutableList<Article> = ArrayList(),
)

class HomeViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
            val articleData: MutableList<Article> = arrayListOf()
            banner.await().data?.let { articleData.add(Article(banners = it, viewType = 0)) }
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
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                response.data?.datas?.let { datas ->
                    state.result.addAll(datas)
                }
                state.copy(
                    refreshing = false,
                    loading = hasNextPage(),
                    finishing = !hasNextPage()
                )
            }
        }
    }

    /**
     * 获取banner
     */
    private suspend fun getBanner(): BannerList {
        return coroutineScope {
            get {
                setUrl("banner/json")
            }
        }
    }

    /**
     * 获取置顶文章
     */
    private suspend fun getArticleTop(): TopArticle {
        return coroutineScope {
            get {
                setUrl("article/top/json")
            }
        }
    }

    /**
     * 获取首页文章列表
     * page 0开始
     */
    private suspend fun getArticleList(page: Int): ArticleList {
        val response = coroutineScope {
            get<ArticleList> {
                setUrl("article/list/{page}/json")
                putPath("page", page.toString())
            }
        }
        return response
    }

}