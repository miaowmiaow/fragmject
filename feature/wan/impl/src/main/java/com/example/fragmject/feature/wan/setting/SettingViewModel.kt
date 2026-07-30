package com.example.fragmject.feature.wan.setting

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.database.model.UserEntity
import com.example.fragmject.core.common.DarkThemeState
import com.example.fragmject.core.database.store.ThemeStore
import com.example.fragmject.core.database.store.UserStore
import com.example.fragmject.core.data.repository.UserRepository
import com.example.fragmject.core.domain.usecase.LogoutUseCase
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
        val user: UserEntity? = null,
        val darkTheme: Boolean = DarkThemeState.isDark.value,
    ) : SettingUiState
}

sealed interface SettingEvent {
    data object LogoutCompleted : SettingEvent
}

// Screen accessors
val SettingUiState.user get() = (this as? SettingUiState.Success)?.user
val SettingUiState.darkTheme get() = (this as? SettingUiState.Success)?.darkTheme ?: false
val SettingUiState.isLoading get() = this is SettingUiState.Loading

private inline fun MutableStateFlow<SettingUiState>.updateData(
    crossinline block: (SettingUiState.Success) -> SettingUiState.Success,
) {
    update {
        val current = (it as? SettingUiState.Success) ?: SettingUiState.Success()
        block(current)
    }
}

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<SettingUiState>(SettingUiState.Success())
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            UserStore.getUser().collect { user ->
                _uiState.updateData { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            DarkThemeState.isDark.collect { dark ->
                _uiState.updateData { it.copy(darkTheme = dark) }
            }
        }
    }

    fun updateDarkTheme(darkTheme: Boolean) {
        viewModelScope.launch { ThemeStore.setDarkTheme(darkTheme) }
    }

    fun logout() {
        val currentUser = (uiState.value as? SettingUiState.Success)?.user
        _uiState.update { SettingUiState.Loading }
        viewModelScope.launch {
            logoutUseCase(currentUser)
            _uiState.updateData { it }
            _events.send(SettingEvent.LogoutCompleted)
        }
    }
}