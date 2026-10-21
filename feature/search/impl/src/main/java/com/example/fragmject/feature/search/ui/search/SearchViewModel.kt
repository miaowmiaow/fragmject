package com.example.fragmject.feature.search.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.repository.SearchRepository
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
    val collectState: CollectState,
) : ViewModel() {

    /** 搜索词驱动搜索结果分页流，搜索词变化时重建 Pager。 */
    private val _searchKey = MutableStateFlow<String?>(null)
    val pagingFlow = _searchKey
        .filterNotNull()
        .flatMapLatest { key -> searchRepository.getSearchPagingData(key) }
        .cachedIn(viewModelScope)

    private val _isShowingSearchResults = MutableStateFlow(false)
    val isShowingSearchResults: StateFlow<Boolean> = _isShowingSearchResults.asStateFlow()

    private val _searchHistoryResult = MutableStateFlow<List<History>>(emptyList())
    val searchHistoryResult: StateFlow<List<History>> = _searchHistoryResult.asStateFlow()

    private val _hotKeyResult = MutableStateFlow<List<HotKey>>(emptyList())
    val hotKeyResult: StateFlow<List<HotKey>> = _hotKeyResult.asStateFlow()

    private val _hotKeyError = MutableStateFlow("")
    val hotKeyError: StateFlow<String> = _hotKeyError.asStateFlow()

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
        viewModelScope.launch {
            refreshHotKey()
        }
    }

    /** 热门搜索错误态重试。 */
    fun retryHotKey() {
        viewModelScope.launch {
            refreshHotKey()
        }
    }

    private suspend fun refreshHotKey() {
        _isHotKeyLoading.value = true
        _hotKeyError.value = ""
        when (val r = searchRepository.refreshHotKey()) {
            // 成功也必须显式收尾：生产实现靠 Room 写入后再次推送 hotKeys 来关闭 loading，
            // 但"刷新成功且数据未变"时不会重发，只靠 observe 收尾会让热搜区永久转圈。
            is DomainResult.Success -> _isHotKeyLoading.value = false
            is DomainResult.Failure -> {
                _isHotKeyLoading.value = false
                _hotKeyError.value = r.message
            }
        }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch {
            historyRepo.deleteHistory(history)
        }
    }

    fun exitSearchResults() {
        _isShowingSearchResults.value = false
        _searchKey.value = null
    }

    fun submitSearch(key: String) {
        viewModelScope.launch {
            historyRepo.recordSearchQuery(key)
        }
        _isShowingSearchResults.value = true
        _searchKey.value = key
    }

}