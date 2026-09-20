package com.example.fragmject.feature.search.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.ui.contract.CollectActionHolder
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.model.History
import com.example.fragmject.core.model.HotKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val historyRepo: HistoryRepository,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel(), CollectActionHolder {

    /** 搜索词驱动搜索结果分页流，搜索词变化时重建 Pager。 */
    private val _searchKey = MutableStateFlow<String?>(null)
    val pagingFlow = _searchKey
        .filterNotNull()
        .flatMapLatest { key -> searchRepository.getSearchPagingData(key) }
        .cachedIn(viewModelScope)

    private val _isSearch = MutableStateFlow(false)
    val isSearch: StateFlow<Boolean> = _isSearch.asStateFlow()

    private val _searchHistoryResult = MutableStateFlow<List<History>>(emptyList())
    val searchHistoryResult: StateFlow<List<History>> = _searchHistoryResult.asStateFlow()

    private val _hotKeyResult = MutableStateFlow<List<HotKey>>(emptyList())
    val hotKeyResult: StateFlow<List<HotKey>> = _hotKeyResult.asStateFlow()

    private val _isHotKeyLoading = MutableStateFlow(true)
    val isHotKeyLoading: StateFlow<Boolean> = _isHotKeyLoading.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepo.observeSearchHistory().collect { history ->
                _searchHistoryResult.value = history
            }
        }
        viewModelScope.launch {
            searchRepository.observeHotKey().collect { hotKeys ->
                _hotKeyResult.value = hotKeys
                _isHotKeyLoading.value = false
            }
        }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch {
            historyRepo.deleteHistory(history)
        }
    }

    fun clearArticles() {
        _isSearch.value = false
        _searchKey.value = null
    }

    fun getHome(key: String) {
        viewModelScope.launch {
            historyRepo.setSearchHistory(key)
        }
        _isSearch.value = true
        _searchKey.value = key
    }

    /** 收藏 / 取消收藏。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}