package com.example.fragment.project.ui.login

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.LoginBean
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.post
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    var isLoading: Boolean = false,
    var errorCode: String = "",
    var errorMsg: String = "",
)

class LoginViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank()) {
            _uiState.update {
                it.copy(errorCode = "${System.nanoTime()}", errorMsg = "用户名不能为空")
            }
            return
        }
        if (password.isBlank()) {
            _uiState.update {
                it.copy(errorCode = "${System.nanoTime()}", errorMsg = "密码不能为空")
            }
            return
        }
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val request = HttpRequest("user/login")
                .putParam("username", username)
                .putParam("password", password)
            val response = post<LoginBean>(request)
            WanHelper.setUser(response.data)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorCode = response.errorCode,
                    errorMsg = response.errorMsg
                )
            }
        }
    }

}