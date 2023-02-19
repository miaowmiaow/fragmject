package com.example.fragment.project.ui.login

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.LoginBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    var isLoading: Boolean = false,
    var result: LoginBean = LoginBean(),
)

class LoginViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginState())

    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val request = HttpRequest("user/login")
                .putParam("username", username)
                .putParam("password", password)
            val response = post<LoginBean>(request) { updateProgress(it) }
            _uiState.update {
                it.copy(isLoading = false, result = response)
            }
        }
    }

}