package com.example.fragmject.feature.user.ui.history

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.History
import com.example.fragmject.core.domain.repository.HistoryRepository
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseHistoryUiState(
    val bookmarkResult: List<History> = emptyList(),
    val historyResult: List<History> = emptyList(),
)

@HiltViewModel
class BrowseHistoryViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseHistoryUiState())
    val uiState: StateFlow<BrowseHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 必须用 combine 而不是 zip：书签与浏览记录来自两张表，写入时只会有一条流发射。
            // zip 按「第 N 个值配第 N 个值」配对，单条流单独发射不会产生新组合值，
            // 表现为「记录了一条浏览历史，列表却不刷新」。combine 会把最新值与本侧旧值重组。
            combine(
                historyRepo.observeBookmarks(),
                historyRepo.observeBrowseHistory(),
            ) { bookmarks, history ->
                BrowseHistoryUiState(bookmarkResult = bookmarks, historyResult = history)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun recordBrowseVisit(title: String, url: String) {
        viewModelScope.launch { historyRepo.recordBrowseVisit(title, url) }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch { historyRepo.deleteHistory(history) }
    }
}
