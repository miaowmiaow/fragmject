package com.example.fragmject.feature.wan.login

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.data.repository.UserRepository
import com.example.fragmject.core.domain.usecase.LoginResult
import com.example.fragmject.core.domain.usecase.LoginUseCase
import com.example.fragmject.core.database.store.UserStore
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NiA 风格 sealed UiState：穷举所有状态，消除 boolean 标志。
 */
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val message: String) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)

    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.update { LoginUiState.Idle }
    }

    fun login(username: String, password: String) {
        _uiState.update { LoginUiState.Loading }
        viewModelScope.launch {
            when (val result = loginUseCase(username, password)) {
                is LoginResult.Success -> _uiState.update { LoginUiState.Success(result.message) }
                is LoginResult.Error -> _uiState.update { LoginUiState.Error(result.message) }
            }
        }
    }
}