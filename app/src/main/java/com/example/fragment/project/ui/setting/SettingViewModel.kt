package com.example.fragment.project.ui.setting

import androidx.appcompat.app.AppCompatDelegate
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
    var mode: Int = -1,
    var user: User = User(),
)

class SettingViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState())

    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    init {
        getUiMode()
        getUser()
    }

    private fun getUiMode() {
        viewModelScope.launch {
            val mode = WanHelper.getUiMode()
            _uiState.update { state ->
                state.copy(mode = mode)
            }
        }
    }

    fun updateUiMode(mode: Int) {
        viewModelScope.launch {
            WanHelper.setUiMode(mode)
            _uiState.update { state ->
                state.copy(mode = mode)
            }
            if (mode != AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    private fun getUser() {
        viewModelScope.launch {
            val user = WanHelper.getUser()
            _uiState.update {
                it.copy(user = user)
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
            if (response.errorCode == "0") {
                val user = User()
                WanHelper.setUser(user)
                _uiState.update {
                    it.copy(isLoading = false, user = user)
                }
            }
        }
    }
}