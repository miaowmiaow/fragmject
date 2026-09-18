package com.example.fragmject.feature.home.ui.nav

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NavUiState {
    data object Loading : NavUiState
    data class Success(
        val navigationResult: List<Navigation> = emptyList(),
    ) : NavUiState
}

// Screen accessors
val NavUiState.navigationResult get() = (this as? NavUiState.Success)?.navigationResult ?: emptyList()
val NavUiState.isLoading get() = this is NavUiState.Loading

private const val TAG = "NavVM"

@HiltViewModel
class NavViewModel @Inject constructor(
    private val navigationRepository: NavigationRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<NavUiState>(NavUiState.Loading)
    val uiState: StateFlow<NavUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            navigationRepository.observeNavigation().collect { nav ->
                _uiState.updateSuccessFrom({ NavUiState.Success() }) { state ->
                    state.copy(navigationResult = nav)
                }
            }
        }
        viewModelScope.launch {
            when (val r = navigationRepository.refreshNavigation()) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> Log.e(TAG, "refreshNavigation failed: ${r.code} ${r.message}")
            }
        }
    }
}