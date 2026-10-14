package com.example.fragmject.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.domain.AuthState
import com.example.fragmject.core.domain.AuthStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 导航层 ViewModel：提供当前登录状态。
 *
 * 通过 core:domain 的 [AuthState] 契约获取登录态，
 * 不再直接依赖 core:domain 的 UserRepository 或 core:model 的 User。
 */
@HiltViewModel
class AppNavViewModel @Inject constructor(
    authState: AuthState,
) : ViewModel() {

    val authStatus: StateFlow<AuthStatus> = authState.status
    internal val pendingRedirectState = mutableStateOf<NavKey?>(null)
    internal val selectedDetailState = mutableStateOf<NavKey?>(null)
}
