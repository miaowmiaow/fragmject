package com.example.fragment.project.ui.register

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.RegisterBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterState(
    var isLoading: Boolean = false,
    var result: RegisterBean = RegisterBean(),
)

class RegisterViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(RegisterState())

    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    fun register(username: String, password: String, repassword: String) {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val request = HttpRequest("user/register")
                .putParam("username", username)
                .putParam("password", password)
                .putParam("repassword", repassword)
            val response = post<RegisterBean>(request) { updateProgress(it) }
            _uiState.update {
                it.copy(isLoading = false, result = response)
            }
        }
    }

}