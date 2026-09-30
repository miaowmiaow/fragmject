package com.example.fragmject.feature.auth

import com.example.fragmject.core.android.platform.AppScope
import com.example.fragmject.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证会话实现：把 [UserRepository.observeCurrentUser] 映射为布尔登录态。
 *
 * 实现 feature:auth:api 的 [AuthSession] 契约，供导航守卫消费；
 * app 层只依赖 auth:api 的 AuthSession，不再 import UserRepository / User 模型。
 */
@Singleton
class AuthSessionImpl @Inject constructor(
    userRepository: UserRepository,
) : AuthSession {
    override val isLoggedIn: StateFlow<Boolean> =
        userRepository.observeCurrentUser()
            .map { it != null && it.id > 0 }
            .stateIn(AppScope, SharingStarted.Eagerly, false)
}
