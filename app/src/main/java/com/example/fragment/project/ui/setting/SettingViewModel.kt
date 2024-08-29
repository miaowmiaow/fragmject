package com.example.fragment.project.ui.setting

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewModelScope
import com.example.fragment.project.database.user.User
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
    var mode: Int = -1,
    var user: User? = null,
)

class SettingViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState())

    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WanHelper.getUser().collect { user ->
                _uiState.update { state ->
                    state.copy(user = user, mode = WanHelper.getUiMode())
                }
            }
        }
    }

    fun updateUiMode(mode: Int) {
        viewModelScope.launch {
            _uiState.update { state ->
                WanHelper.setUiMode(mode)
                state.copy(mode = mode)
            }
            if (mode != AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.setDefaultNightMode(mode)
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