package com.example.fragmject.feature.user.ui.setting

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.utils.CacheUtils
import com.example.fragmject.core.common.utils.FileUtil
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.model.User
import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface SettingUiState {
    data object Loading : SettingUiState
    data class Success(
        val user: User? = null,
        val darkTheme: Boolean = false,
    ) : SettingUiState
}

sealed interface SettingEvent {
    data object LogoutCompleted : SettingEvent
}

// Screen accessors
val SettingUiState.user get() = (this as? SettingUiState.Success)?.user
val SettingUiState.darkTheme get() = (this as? SettingUiState.Success)?.darkTheme ?: false
val SettingUiState.isLoading get() = this is SettingUiState.Loading

@HiltViewModel
class SettingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logoutUseCase: LogoutUseCase,
    private val userRepo: UserRepository,
    private val themeRepo: ThemeRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<SettingUiState>(SettingUiState.Success())
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    private val _cacheSize = MutableStateFlow("0KB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _events = Channel<SettingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
        refreshCacheSize()
    }

    fun updateDarkTheme(darkTheme: Boolean) {
        viewModelScope.launch { themeRepo.setDarkTheme(darkTheme) }
    }

    /** 刷新缓存大小显示（IO 下沉到 ViewModel）。 */
    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = CacheUtils.getTotalSize(context)
        }
    }

    /** 抹除数据：写入脏数据干扰隐私窃取（IO 下沉到 ViewModel）。 */
    fun eraseData() {
        FileUtil.writeDirtyRead(
            File(CacheUtils.getDirPath(context, "org"), "DirtyRead")
        )
    }

    /** 清除缓存后刷新缓存大小显示（IO 下沉到 ViewModel）。 */
    fun clearCache() {
        viewModelScope.launch {
            CacheUtils.clearAllCache(context)
            _cacheSize.value = CacheUtils.getTotalSize(context)
        }
    }

    fun logout() {
        _uiState.update { SettingUiState.Loading }
        viewModelScope.launch {
            logoutUseCase()
            _uiState.updateSuccessFrom({ SettingUiState.Success() }) { it }
            _events.send(SettingEvent.LogoutCompleted)
        }
    }
}