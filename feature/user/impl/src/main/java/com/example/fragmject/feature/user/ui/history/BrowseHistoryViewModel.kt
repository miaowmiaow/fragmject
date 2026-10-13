package com.example.fragmject.feature.user.ui.history

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.History
import com.example.fragmject.core.domain.repository.HistoryRepository
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
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
            historyRepo.observeBookmarks().zip(historyRepo.observeBrowseHistory()) { b, h ->
                _uiState.update { it.copy(bookmarkResult = b, historyResult = h) }
            }.collect {}
        }
    }

    fun recordBrowseVisit(title: String, url: String) {
        viewModelScope.launch { historyRepo.recordBrowseVisit(title, url) }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch { historyRepo.deleteHistory(history) }
    }
}
