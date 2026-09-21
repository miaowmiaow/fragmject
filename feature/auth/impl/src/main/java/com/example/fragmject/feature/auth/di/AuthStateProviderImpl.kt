package com.example.fragmject.feature.auth.di

import com.example.fragmject.core.common.utils.AppScope
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.navigation.AuthStateProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 登录态契约实现：把 [UserRepository.observeCurrentUser] 映射为布尔登录态。
 *
 * 放在 feature:auth:impl 而不是 app，使 app 只依赖 core:navigation 的
 * [AuthStateProvider]，不再 import UserRepository / User 模型。
 */
@Singleton
class AuthStateProviderImpl @Inject constructor(
    userRepository: UserRepository,
) : AuthStateProvider {
    override val isLoggedIn: StateFlow<Boolean> =
        userRepository.observeCurrentUser()
            .map { it != null && it.id > 0 }
            .stateIn(AppScope, SharingStarted.Eagerly, false)
}
