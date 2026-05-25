package com.example.fragment.project.ui.register

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

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isLogin: Boolean = false,
    val message: String = "",
)

class RegisterViewModel(
    private val userRepo: UserRepository = WanRepositoryProvider.user,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())

    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.update {
            it.copy(message = "")
        }
    }

    fun register(username: String, password: String, repassword: String) {
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
        if (repassword.isBlank()) {
            _uiState.update {
                it.copy(message = "确认密码不能为空")
            }
            return
        }
        if (password != repassword) {
            _uiState.update {
                it.copy(message = "两次密码不一样")
            }
            return
        }
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val response = userRepo.register(username, password, repassword)
            _uiState.update {
                response.data?.let { user ->
                    WanHelper.setUser(user)
                }
                it.copy(
                    isLoading = false,
                    isLogin = response.errorCode == "0",
                    message = response.errorMsg
                )
            }
        }
    }

}