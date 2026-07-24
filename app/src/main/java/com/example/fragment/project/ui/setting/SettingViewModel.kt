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
    val darkTheme: Boolean = WanHelper.darkTheme.value,
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
        viewModelScope.launch {
            WanHelper.darkTheme.collect { dark ->
                _uiState.update { state ->
                    state.copy(darkTheme = dark)
                }
            }
        }
    }

    fun updateDarkTheme(darkTheme: Boolean) {
        viewModelScope.launch {
            WanHelper.setDarkTheme(darkTheme)
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