package com.example.fragmject.app.navigation

import androidx.lifecycle.ViewModel
import com.example.fragmject.feature.auth.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 导航层 ViewModel：提供当前登录状态。
 *
 * 通过 feature:auth:api 的 [AuthSession] 契约获取登录态，
 * 不再直接依赖 core:domain 的 UserRepository 或 core:model 的 User。
 */
@HiltViewModel
class AppNavViewModel @Inject constructor(
    authSession: AuthSession,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authSession.isLoggedIn
}
