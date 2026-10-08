package com.example.fragmject.feature.user.ui.setting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.ui.utils.updateSuccessFrom
import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.system.SystemStorage
import com.example.fragmject.core.domain.usecase.LogoutUseCase
import com.example.fragmject.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingUiState {
    data object Loading : SettingUiState
    data class Success(
        val user: User? = null,
        val darkTheme: Boolean = false,
    ) : SettingUiState
}

sealed interface SettingEvent {
    data object LogoutSucceeded : SettingEvent
    data object LogoutFailed : SettingEvent
}

// Screen accessors
val SettingUiState.user get() = (this as? SettingUiState.Success)?.user
val SettingUiState.darkTheme get() = (this as? SettingUiState.Success)?.darkTheme ?: false
val SettingUiState.isLoading get() = this is SettingUiState.Loading

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val userRepo: UserRepository,
    private val themeRepo: ThemeRepository,
    private val systemStorage: SystemStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingUiState>(SettingUiState.Success())
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** 缓存大小展示文本；读写经由 [SystemStorage]，UI 不接触缓存目录。 */
    val cacheSizeText: StateFlow<String> get() = systemStorage.cacheSizeText

    /** 是否正在清理缓存：用于禁用入口，避免重复触发清理。 */
    val isClearing: StateFlow<Boolean> get() = systemStorage.isClearing

    init {
        viewModelScope.launch {
            userRepo.observeCurrentUser().collect { user ->
                _uiState.updateSuccessFrom({ SettingUiState.Success() }) { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            themeRepo.observeDarkTheme().collect { dark ->
                _uiState.updateSuccessFrom({ SettingUiState.Success() }) { it.copy(darkTheme = dark) }
            }
        }
        viewModelScope.launch { systemStorage.refreshCacheSize() }
    }

    fun updateDarkTheme(darkTheme: Boolean) {
        viewModelScope.launch { themeRepo.setDarkTheme(darkTheme) }
    }

    /** 刷新缓存大小显示：读写经由 [SystemStorage]，UI 不接触缓存目录。 */
    fun refreshCacheSize() {
        viewModelScope.launch { systemStorage.refreshCacheSize() }
    }

    /** 清除缓存：清理范围等业务规则由 [SystemStorage] 实现承担。 */
    fun clearCache() {
        viewModelScope.launch { systemStorage.clearCache() }
    }

    fun logout() {
        _uiState.update { SettingUiState.Loading }
        viewModelScope.launch {
            try {
                val success = logoutUseCase()
                _uiState.updateSuccessFrom({ SettingUiState.Success() }) { it }
                _events.send(if (success) SettingEvent.LogoutSucceeded else SettingEvent.LogoutFailed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "logout failed", e)
                _uiState.updateSuccessFrom({ SettingUiState.Success() }) { it }
                _events.send(SettingEvent.LogoutFailed)
            }
        }
    }

    private companion object {
        const val TAG = "SettingViewModel"
    }
}
