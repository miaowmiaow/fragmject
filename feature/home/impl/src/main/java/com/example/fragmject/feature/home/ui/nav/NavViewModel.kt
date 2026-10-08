package com.example.fragmject.feature.home.ui.nav

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.NavTab
import com.example.fragmject.core.domain.repository.HomeNavRepository
import com.example.fragmject.core.domain.result.DomainResult
import androidx.lifecycle.ViewModel
import com.example.fragmject.core.ui.utils.updateSuccessFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NavUiState {
    data object Loading : NavUiState
    data class Success(
        val navigationResult: List<NavTab> = emptyList(),
    ) : NavUiState
    data class Error(
        val code: String = "",
        val message: String = "",
    ) : NavUiState
}

// Screen accessors
val NavUiState.navigationResult get() = (this as? NavUiState.Success)?.navigationResult ?: emptyList()
val NavUiState.isLoading get() = this is NavUiState.Loading
val NavUiState.errorMessage get() = (this as? NavUiState.Error)?.message ?: ""

@HiltViewModel
class NavViewModel @Inject constructor(
    private val homeNavRepository: HomeNavRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NavUiState>(NavUiState.Loading)
    val uiState: StateFlow<NavUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            homeNavRepository.observeNavigation().collect { nav ->
                _uiState.updateSuccessFrom({ NavUiState.Success() }) { state ->
                    state.copy(navigationResult = nav)
                }
            }
        }
        viewModelScope.launch {
            when (val r = homeNavRepository.refreshNavigation()) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure ->
                    _uiState.value = NavUiState.Error(r.code, r.message)
            }
        }
    }
}