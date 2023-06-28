package com.example.fragment.project.ui.search

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.ArticleListBean
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var historyResult: MutableList<String> = ArrayList(),
    var articlesResult: MutableList<ArticleBean> = ArrayList(),
    var updateTime: Long = 0
)

class SearchViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SearchState())

    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    init {
        getSearchHistory()
    }

    private fun getSearchHistory() {
        WanHelper.getSearchHistory { history ->
            _uiState.update {
                it.historyResult.addAll(history)
                it.copy(updateTime = System.nanoTime())
            }
        }
    }

    fun updateSearchHistory(key: String) {
        _uiState.update {
            if (it.historyResult.contains(key)) {
                it.historyResult.remove(key)
            }
            it.historyResult.add(0, key)
            WanHelper.setSearchHistory(it.historyResult)
            it.copy(updateTime = System.nanoTime())
        }
    }

    fun removeSearchHistory(key: String) {
        _uiState.update {
            if (it.historyResult.contains(key)) {
                it.historyResult.remove(key)
            }
            WanHelper.setSearchHistory(it.historyResult)
            it.copy(updateTime = System.nanoTime())
        }
    }

    fun clearArticles() {
        _uiState.update {
            it.articlesResult.clear()
            it.copy(updateTime = System.nanoTime())
        }
    }

    fun getHome(key: String) {
        _uiState.update {
            it.copy(refreshing = true)
        }
        getArticleQuery(key, getHomePage())
    }

    fun getNext(key: String) {
        _uiState.update {
            it.copy(loading = false)
        }
        getArticleQuery(key, getNextPage())
    }

    /**
     * 搜索
     * k 搜索关键词
     * page 0开始
     */
    private fun getArticleQuery(key: String, page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("article/query/{page}/json").putParam("k", key).putPath("page", page.toString())
            //以get方式发起网络请求
            val response = post<ArticleListBean>(request)
            //根据接口返回更新总页码
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                response.data?.datas?.let { datas ->
                    if (isHomePage()) {
                        state.articlesResult.clear()
                    }
                    datas.forEach {
                        state.articlesResult.add(it.build())
                    }
                }
                state.copy(refreshing = false, loading = hasNextPage())
            }
        }
    }

}