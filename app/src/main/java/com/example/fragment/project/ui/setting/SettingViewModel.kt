package com.example.fragment.project.ui.setting

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.User
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.http.HttpResponse
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingUiState(
    var isLoading: Boolean = false,
    var user: User? = null,
)

class SettingViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState())

    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WanHelper.getUser().collect { user ->
                _uiState.update { state ->
                    state.copy(user = user)
                }
            }
        }
    }

    fun updateDarkTheme(darkTheme: Boolean) {
        viewModelScope.launch {
            _uiState.value.user?.let { user ->
                user.darkTheme = darkTheme
                WanHelper.setUser(user)
            }
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val response = get<HttpResponse> {
                setUrl("user/logout/json")
            }
            _uiState.update { state ->
                if (response.errorCode == "0") {
                    state.user?.let { user ->
                        WanHelper.deleteUser(user)
                    }
                }
                state.copy(isLoading = false)
            }
        }
    }
}