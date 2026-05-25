package com.example.fragment.project.ui.setting

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.User
import com.example.fragment.project.data.repository.UserRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
)

class SettingViewModel(
    private val userRepo: UserRepository = WanRepositoryProvider.user,
) : BaseViewModel() {

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
            // 使用 copy 生成新实例，避免直接突变 data class 字段，
            // 这样 StateFlow 的订阅方也能感知到变化。
            _uiState.value.user?.copy(darkTheme = darkTheme.toString())?.let { updated ->
                WanHelper.setUser(updated)
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
            val response = userRepo.logout()
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