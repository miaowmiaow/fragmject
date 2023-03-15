package com.example.fragment.project.ui.login

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.LoginBean
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    var isLoading: Boolean = false,
    var errorCode: String = "-1",
    var errorMsg: String = "",
    var time: Long = 0
)

class LoginViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginState())

    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank()) {
            _uiState.update {
                it.copy(errorMsg = "用户名不能为空", time = System.currentTimeMillis())
            }
            return
        }
        if (password.isBlank()) {
            _uiState.update {
                it.copy(errorMsg = "密码不能为空", time = System.currentTimeMillis())
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorCode = "-1", errorMsg = "") }
        viewModelScope.launch {
            val request = HttpRequest("user/login")
                .putParam("username", username)
                .putParam("password", password)
            val response = post<LoginBean>(request) { updateProgress(it) }
            response.data?.let { WanHelper.setUser(it) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorCode = response.errorCode,
                    errorMsg = response.errorMsg,
                    time = System.currentTimeMillis()
                )
            }
        }
    }

}