package com.example.fragmject.feature.wan.main.nav

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NavUiState {
    data object Loading : NavUiState
    data class Success(
        val navigationResult: List<Navigation> = emptyList(),
        val systemTreeResult: List<Tree> = emptyList(),
    ) : NavUiState
}

// Screen accessors
val NavUiState.navigationResult get() = (this as? NavUiState.Success)?.navigationResult ?: emptyList()
val NavUiState.isLoading get() = this is NavUiState.Loading

private inline fun MutableStateFlow<NavUiState>.updateData(
    crossinline block: (NavUiState.Success) -> NavUiState.Success,
) {
    update {
        val current = (it as? NavUiState.Success) ?: NavUiState.Success()
        block(current)
    }
}

@HiltViewModel
class NavViewModel @Inject constructor(
    private val offlineFirst: com.example.fragmject.core.data.repository.OfflineFirstCommonRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<NavUiState>(NavUiState.Loading)
    val uiState: StateFlow<NavUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            offlineFirst.observeNavigation().collect { nav ->
                _uiState.updateData { state ->
                    state.copy(navigationResult = nav)
                }
            }
        }
        viewModelScope.launch {
            offlineFirst.observeSystemTree().collect { trees ->
                _uiState.updateData { state ->
                    state.copy(systemTreeResult = trees)
                }
            }
        }
        viewModelScope.launch { offlineFirst.refreshAll() }
    }
}