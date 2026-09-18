package com.example.fragmject.feature.search.ui.search

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.CollectActionHolder
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.SearchArticlesUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.History
import com.example.fragmject.core.model.HotKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data class Success(
        val isSearch: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        val searchHistoryResult: List<History> = emptyList(),
        val articlesResult: List<Article> = emptyList(),
        val hotKeyResult: List<HotKey> = emptyList(),
        val isHotKeyLoading: Boolean = true,
    ) : SearchUiState
}


val SearchUiState.isSearch get() = (this as? SearchUiState.Success)?.isSearch ?: false
val SearchUiState.isRefreshing get() = (this as? SearchUiState.Success)?.isRefreshing ?: false
val SearchUiState.isLoading get() = (this as? SearchUiState.Success)?.isLoading ?: false
val SearchUiState.isFinishing get() = (this as? SearchUiState.Success)?.isFinishing ?: false
val SearchUiState.searchHistoryResult get() = (this as? SearchUiState.Success)?.searchHistoryResult ?: emptyList()
val SearchUiState.articlesResult get() = (this as? SearchUiState.Success)?.articlesResult ?: emptyList()
val SearchUiState.hotKeyResult get() = (this as? SearchUiState.Success)?.hotKeyResult ?: emptyList()
val SearchUiState.isHotKeyLoading get() = (this as? SearchUiState.Success)?.isHotKeyLoading ?: true

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchArticles: SearchArticlesUseCase,
    private val historyRepo: HistoryRepository,
    private val collectArticle: CollectArticleUseCase,
    private val searchRepository: SearchRepository,
) : BaseViewModel(), CollectActionHolder {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Success())

    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepo.observeSearchHistory().collect { history ->
                _uiState.updateSuccessFrom({ SearchUiState.Success() }) { state ->
                    state.copy(searchHistoryResult = history)
                }
            }
        }
        viewModelScope.launch {
            searchRepository.observeHotKey().collect { hotKeys ->
                _uiState.updateSuccessFrom({ SearchUiState.Success() }) { state ->
                    state.copy(hotKeyResult = hotKeys, isHotKeyLoading = false)
                }
            }
        }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch {
            historyRepo.deleteHistory(history)
        }
    }

    fun clearArticles() {
        _uiState.updateSuccessFrom({ SearchUiState.Success() }) { it.copy(isSearch = false, articlesResult = emptyList())
        }
    }

    fun getHome(key: String, userTriggered: Boolean = false) {
        viewModelScope.launch {
            historyRepo.setSearchHistory(key)
        }
        _uiState.updateSuccessFrom({ SearchUiState.Success() }) { it.copy(isSearch = true, isRefreshing = userTriggered, isLoading = false, isFinishing = false)
        }
        getList(key, getHomePage())
    }

    fun getNext(key: String) {
        _uiState.updateSuccessFrom({ SearchUiState.Success() }) { it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
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
            when (val result = searchArticles(key, page)) {
                is DomainResult.Success -> {
                    val response = result.data
                    val datas = response.articles
                    val paging = finishPage(response.pageCount)
                    _uiState.updateSuccessFrom({ SearchUiState.Success() }) { state ->
                        val merged = if (isHomePage()) datas else state.articlesResult + datas
                        state.copy(
                            isRefreshing = false,
                            isLoading = paging.isLoading,
                            isFinishing = paging.isFinishing,
                            articlesResult = merged
                        )
                    }
                }

                is DomainResult.Failure -> {
                    _uiState.updateSuccessFrom({ SearchUiState.Success() }) { state ->
                        state.copy(
                            isRefreshing = false,
                            isLoading = false,
                            isFinishing = true,
                        )
                    }
                }
            }
        }
    }

    /** 收藏 / 取消收藏。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }

}