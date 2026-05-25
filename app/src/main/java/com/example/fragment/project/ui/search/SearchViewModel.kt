package com.example.fragment.project.ui.search

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.History
import com.example.fragment.project.data.repository.ArticleRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val isSearch: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val searchHistoryResult: List<History> = emptyList(),
    val articlesResult: List<Article> = emptyList(),
)

class SearchViewModel(
    private val articleRepo: ArticleRepository = WanRepositoryProvider.article,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())

    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WanHelper.getSearchHistory().collect { history ->
                _uiState.update { state ->
                    state.copy(searchHistoryResult = history)
                }
            }
        }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch {
            WanHelper.deleteHistory(history)
        }
    }

    fun clearArticles() {
        _uiState.update {
            it.copy(isSearch = false, articlesResult = emptyList())
        }
    }

    fun getHome(key: String) {
        viewModelScope.launch {
            WanHelper.setSearchHistory(key)
        }
        _uiState.update {
            it.copy(isSearch = true, isRefreshing = true, isLoading = false, isFinishing = false)
        }
        getList(key, getHomePage())
    }

    fun getNext(key: String) {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        getList(key, getNextPage())
    }

    /**
     * 搜索
     * k 搜索关键词
     * page 0开始
     */
    private fun getList(key: String, page: Int) {
        viewModelScope.launch {
            val response = articleRepo.searchArticles(key, page)
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                val datas = response.data?.datas.orEmpty()
                val merged = if (isHomePage()) datas else state.articlesResult + datas
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    articlesResult = merged
                )
            }
        }
    }

}