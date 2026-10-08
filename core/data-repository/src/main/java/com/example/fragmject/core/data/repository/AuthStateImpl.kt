package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.AuthState
import com.example.fragmject.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证状态实现：把 [UserRepository.observeCurrentUser] 映射为布尔登录态。
 *
 * 下沉到 core:data-repository（领域端口装配层），与 CollectState / ThemeState 归位方式一致；
 * app 层只依赖 core:domain 的 AuthState，不再 import UserRepository / User 模型。
 */
@Singleton
class AuthStateImpl @Inject constructor(
    userRepository: UserRepository,
    appScope: AppCoroutineScope,
) : AuthState {
    override val isLoggedIn: StateFlow<Boolean> =
        userRepository.observeCurrentUser()
            .map { it != null && it.id > 0 }
            .stateIn(appScope, SharingStarted.Eagerly, false)
}
