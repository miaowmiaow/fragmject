package com.example.fragment.project.ui.login

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.repository.UserRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLogin: Boolean = false,
    val message: String = "",
)

class LoginViewModel(
    private val userRepo: UserRepository = WanRepositoryProvider.user,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.update {
            it.copy(message = "")
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank()) {
            _uiState.update {
                it.copy(message = "用户名不能为空")
            }
            return
        }
        if (password.isBlank()) {
            _uiState.update {
                it.copy(message = "密码不能为空")
            }
            return
        }
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val response = userRepo.login(username, password)
            _uiState.update { state ->
                response.data?.let { user ->
                    WanHelper.setUser(user)
                }
                state.copy(
                    isLoading = false,
                    isLogin = response.errorCode == "0",
                    message = response.errorMsg
                )
            }
        }
    }

}