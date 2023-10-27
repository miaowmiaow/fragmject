package com.example.fragment.project.ui.register

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.RegisterBean
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.http.post
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    var isLoading: Boolean = false,
    var success: Boolean = false,
    var message: String = "",
)

class RegisterViewModel : BaseViewModel() {

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
            val response = post<RegisterBean> {
                setUrl("user/register")
                putParam("username", username)
                putParam("password", password)
                putParam("repassword", repassword)
            }
            WanHelper.setUser(response.data)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    success = response.errorCode == "0",
                    message = response.errorMsg
                )
            }
        }
    }

}