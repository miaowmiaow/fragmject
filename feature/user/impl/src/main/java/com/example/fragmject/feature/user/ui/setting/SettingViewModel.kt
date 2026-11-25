package com.example.fragmject.feature.user.ui.setting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.ui.utils.updateSuccessFrom
import com.example.fragmject.core.domain.repository.FontScaleRepository
import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.system.SystemStorage
import com.example.fragmject.core.domain.text.FontScale
import com.example.fragmject.core.domain.usecase.LogoutUseCase
import com.example.fragmject.core.model.User
import com.example.fragmject.core.webview.WebCacheSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

sealed interface SettingUiState {
    data object Loading : SettingUiState
    data class Success(
        val user: User? = null,
        val darkTheme: Boolean = false,
        val bodyFontSp: Float = FontScale.DEFAULT_BODY_SP,
    ) : SettingUiState
}

sealed interface SettingEvent {
    data object LogoutSucceeded : SettingEvent
    data object LogoutFailed : SettingEvent
    data class CacheClearFailed(val message: String) : SettingEvent
}

// Screen accessors
val SettingUiState.user get() = (this as? SettingUiState.Success)?.user
val SettingUiState.darkTheme get() = (this as? SettingUiState.Success)?.darkTheme ?: false
val SettingUiState.bodyFontSp get() = (this as? SettingUiState.Success)?.bodyFontSp ?: FontScale.DEFAULT_BODY_SP
val SettingUiState.isLoading get() = this is SettingUiState.Loading

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val logoutUseCase: LogoutUseCase,
    private val themeRepo: ThemeRepository,
    private val fontScaleRepo: FontScaleRepository,
    private val systemStorage: SystemStorage,
    private val webCacheSettings: WebCacheSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingUiState>(SettingUiState.Success())
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** 缓存大小展示文本；读写经由 [SystemStorage]，UI 不接触缓存目录。 */
    val cacheSizeText: StateFlow<String> get() = systemStorage.cacheSizeText

    /** 是否正在清理缓存：用于禁用入口，避免重复触发清理。 */
    val isClearing: StateFlow<Boolean> get() = systemStorage.isClearing

    /**
     * 「用登录态缓存第三方图片」开关。
     *
     * 缓存策略归 `core:webview` 所有，这里只做透传：关闭后由缓存实现清掉已有的带凭据条目，
     * 匿名条目不受影响（命中率只损失「需要登录才能拿到」的那部分）。
     */
    val credentialedCacheEnabled: StateFlow<Boolean> get() = webCacheSettings.credentialedCachingEnabled

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
        viewModelScope.launch {
            fontScaleRepo.observeBodyFontSp().collect { sp ->
                _uiState.updateSuccessFrom({ SettingUiState.Success() }) { it.copy(bodyFontSp = sp) }
            }
        }
        viewModelScope.launch { systemStorage.refreshCacheSize() }
    }

    fun updateDarkTheme(darkTheme: Boolean) {
        viewModelScope.launch { themeRepo.setDarkTheme(darkTheme) }
    }

    /** 正文字号：滑块松手时提交，越界夹紧由 FontScaleRepository 兜住。 */
    fun updateBodyFontSp(sp: Float) {
        viewModelScope.launch { fontScaleRepo.setBodyFontSp(sp) }
    }

    fun setCredentialedCacheEnabled(enabled: Boolean) {
        webCacheSettings.setCredentialedCachingEnabled(enabled)
    }

    /** 刷新缓存大小显示：读写经由 [SystemStorage]，UI 不接触缓存目录。 */
    fun refreshCacheSize() {
        viewModelScope.launch { systemStorage.refreshCacheSize() }
    }

    /** 清除缓存：清理范围等业务规则由 [SystemStorage] 实现承担。 */
    fun clearCache() {
        viewModelScope.launch {
            val result = systemStorage.clearCache()
            if (!result.success) {
                _events.send(SettingEvent.CacheClearFailed(result.message.ifBlank { "清除缓存失败，请重试" }))
            }
        }
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
            } catch (e: IOException) {
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
