package com.example.fragment.project.ui.setting

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.UserBean
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
    var darkTheme: Boolean = false,
    var userBean: UserBean = UserBean(),
)

class SettingViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState())

    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    init {
        getUiMode()
        getUser()
    }

    private fun getUiMode() {
        WanHelper.getUiMode {
            if (it == "1") {
                _uiState.update { state ->
                    state.copy(darkTheme = false)
                }
            } else if (it == "2") {
                _uiState.update { state ->
                    state.copy(darkTheme = true)
                }
            }
        }
    }

    fun updateUiMode(darkTheme: Boolean) {
        if (darkTheme) {
            WanHelper.setUiMode("2")
        } else {
            WanHelper.setUiMode("1")
        }
        _uiState.update { state ->
            state.copy(darkTheme = darkTheme)
        }
    }

    private fun getUser() {
        WanHelper.getUser { userBean ->
            _uiState.update {
                it.copy(userBean = userBean)
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
                val userBean = UserBean()
                WanHelper.setUser(userBean)
                _uiState.update {
                    it.copy(isLoading = false, userBean = userBean)
                }
            }
        }
    }
}