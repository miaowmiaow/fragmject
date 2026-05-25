package com.example.fragment.project.ui.main.home

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.ArticleList
import com.example.fragment.project.data.repository.ArticleRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 不可变 UiState：所有字段均为 val，list 通过 copy 时生成新引用，
 * 这样 StateFlow 的 distinctUntilChanged 才能正确触发 Compose 重组。
 */
data class HomeUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val result: List<Article> = emptyList(),
)

class HomeViewModel(
    // 通过默认参数注入 Repository，单测时可传入替身实现
    private val articleRepo: ArticleRepository = WanRepositoryProvider.article,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(isRefreshing = true, isLoading = false, isFinishing = false)
        }
        viewModelScope.launch {
            //通过async获取首页需要展示的数据
            val banner = async { articleRepo.getBanner() }
            val articleTop = async { articleRepo.getArticleTop() }
            val articleList = async { fetchArticleList(getHomePage()) }
            val articleData: MutableList<Article> = arrayListOf()
            banner.await().data?.let { articleData.add(Article(banners = it, viewType = 0)) }
            articleTop.await().data?.onEach { it.top = true }?.let { articleData.addAll(it) }
            articleList.await().data?.datas?.let { articleData.addAll(it) }
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    result = articleData.toList()
                )
            }
        }
    }

    fun getNext() {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        viewModelScope.launch {
            val response = fetchArticleList(getNextPage())
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                val appended = response.data?.datas
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    // 始终生成新 List 引用，确保订阅方能感知列表变化
                    result = if (appended.isNullOrEmpty()) state.result else state.result + appended
                )
            }
        }
    }

    /**
     * 拉取首页文章列表并同步分页元信息（page 从 0 开始）。
     */
    private suspend fun fetchArticleList(page: Int): ArticleList = coroutineScope {
        val response = articleRepo.getArticleList(page)
        updatePageCont(response.data?.pageCount?.toInt())
        response
    }
}