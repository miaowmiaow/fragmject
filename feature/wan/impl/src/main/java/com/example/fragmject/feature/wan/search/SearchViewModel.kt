package com.example.fragmject.feature.wan.search

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.result.AppResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.database.model.HistoryEntity
import com.example.fragmject.core.data.repository.SearchRepository
import com.example.fragmject.core.domain.usecase.SearchArticlesUseCase
import com.example.fragmject.core.database.store.HistoryStore
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data class Success(
val isSearch: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val searchHistoryResult: List<HistoryEntity> = emptyList(),
    val articlesResult: List<Article> = emptyList(),
    ) : SearchUiState
}


val SearchUiState.isSearch get() = (this as? SearchUiState.Success)?.isSearch ?: false
val SearchUiState.isRefreshing get() = (this as? SearchUiState.Success)?.isRefreshing ?: false
val SearchUiState.isLoading get() = (this as? SearchUiState.Success)?.isLoading ?: false
val SearchUiState.isFinishing get() = (this as? SearchUiState.Success)?.isFinishing ?: false
val SearchUiState.searchHistoryResult get() = (this as? SearchUiState.Success)?.searchHistoryResult ?: emptyList()
val SearchUiState.articlesResult get() = (this as? SearchUiState.Success)?.articlesResult ?: emptyList()

private inline fun MutableStateFlow<SearchUiState>.updateData(
    crossinline block: (SearchUiState.Success) -> SearchUiState.Success,
) {
    update { if (it is SearchUiState.Success) block(it) else it }
}
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepo: SearchRepository,
    private val searchArticles: SearchArticlesUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Success())

    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            HistoryStore.getSearchHistory().collect { history ->
                _uiState.updateData { state ->
                    state.copy(searchHistoryResult = history)
                }
            }
        }
    }

    fun deleteHistory(history: HistoryEntity) {
        viewModelScope.launch {
            HistoryStore.deleteHistory(history)
        }
    }

    fun clearArticles() {
        _uiState.updateData { it.copy(isSearch = false, articlesResult = emptyList())
        }
    }

    fun getHome(key: String) {
        viewModelScope.launch {
            HistoryStore.setSearchHistory(key)
        }
        _uiState.updateData { it.copy(isSearch = true, isRefreshing = true, isLoading = false, isFinishing = false)
        }
        getList(key, getHomePage())
    }

    fun getNext(key: String) {
        _uiState.updateData { it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
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
                is AppResult.Success -> {
                    val response = result.data
                    val datas = response.data?.datas.orEmpty()
                    datas.onEach { it.preloadForDisplay() }
                    updatePageCont(response.data?.pageCount?.toInt())
                    _uiState.updateData { state ->
                        val merged = if (isHomePage()) datas else state.articlesResult + datas
                        state.copy(
                            isRefreshing = false,
                            isLoading = hasNextPage(),
                            isFinishing = !hasNextPage(),
                            articlesResult = merged
                        )
                    }
                }

                is AppResult.Loading -> {}
                is AppResult.Failure -> {
                    _uiState.updateData { state ->
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

}