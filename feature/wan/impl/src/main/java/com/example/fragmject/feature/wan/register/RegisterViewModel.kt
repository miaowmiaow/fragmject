package com.example.fragmject.feature.wan.register

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.domain.usecase.RegisterResult
import com.example.fragmject.core.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Success(val message: String) : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)

    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.update { RegisterUiState.Idle }
    }

    fun register(username: String, password: String, repassword: String) {
        _uiState.update { RegisterUiState.Loading }
        viewModelScope.launch {
            when (val result = registerUseCase(username, password, repassword)) {
                is RegisterResult.Success -> _uiState.update { RegisterUiState.Success(result.message) }
                is RegisterResult.Error -> _uiState.update { RegisterUiState.Error(result.message) }
            }
        }
    }
}