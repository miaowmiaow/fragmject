package com.example.fragmject.feature.home.ui.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Tree
import androidx.lifecycle.ViewModel
import com.example.fragmject.core.ui.utils.updateSuccessFrom
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.MainHeaderAggregateUseCase
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
    private val headerAggregate: MainHeaderAggregateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Success(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            headerAggregate.hotKeys.collect { hotKeys ->
                _uiState.updateSuccessFrom({ MainUiState.Success() }) { it.copy(hotKeyResult = hotKeys) }
            }
        }
        viewModelScope.launch {
            headerAggregate.trees.collect { trees ->
                _uiState.updateSuccessFrom({ MainUiState.Success() }) { it.copy(treeResult = trees) }
            }
        }
        viewModelScope.launch {
            when (val r = headerAggregate.refreshAll()) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> Log.e(TAG, "refresh header failed: ${r.code} ${r.message}")
            }
            _uiState.updateSuccessFrom({ MainUiState.Success() }) { it.copy(isLoading = false) }
        }
    }
}
