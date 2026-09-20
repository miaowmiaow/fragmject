package com.example.fragmject.feature.home.ui.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Tree
import androidx.lifecycle.ViewModel
import com.example.fragmject.core.common.utils.updateSuccessFrom
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 不可变 UiState：所有字段使用 val + 不可变集合，
 * 避免直接突变 data class 字段导致 StateFlow 订阅方收不到变化。
 */
sealed interface MainUiState {
    data class Success(
        val hotKeyResult: List<HotKey> = emptyList(),
        val treeResult: List<Tree> = emptyList(),
        val isLoading: Boolean = false,
    ) : MainUiState
}

// Screen accessors
val MainUiState.hotKeyResult get() = (this as? MainUiState.Success)?.hotKeyResult ?: emptyList()
val MainUiState.treeResult get() = (this as? MainUiState.Success)?.treeResult ?: emptyList()
val MainUiState.isLoading get() = (this as? MainUiState.Success)?.isLoading ?: false

private const val TAG = "MainVM"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navigationRepository: NavigationRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Success(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            searchRepository.observeHotKey().collect { hotKeys ->
                _uiState.updateSuccessFrom({ MainUiState.Success() }) { it.copy(hotKeyResult = hotKeys) }
            }
        }
        viewModelScope.launch {
            navigationRepository.observeSystemTree().collect { trees ->
                _uiState.updateSuccessFrom({ MainUiState.Success() }) { it.copy(treeResult = trees) }
            }
        }
        viewModelScope.launch {
            when (val r = navigationRepository.refreshSystemTree()) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> Log.e(TAG, "refreshSystemTree failed: ${r.code} ${r.message}")
            }
            when (val r = searchRepository.refreshHotKey()) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> Log.e(TAG, "refreshHotKey failed: ${r.code} ${r.message}")
            }
            _uiState.updateSuccessFrom({ MainUiState.Success() }) { it.copy(isLoading = false) }
        }
    }
}
