package com.example.fragment.project.ui.setting

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.UserBean
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingState(
    var isLoading: Boolean = false,
    var userBean: UserBean = UserBean(),
)

class SettingViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingState())

    val uiState: StateFlow<SettingState> = _uiState.asStateFlow()

    init {
        getUser()
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
            val request = HttpRequest("user/logout/json")
            val response = get<HttpResponse>(request)
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