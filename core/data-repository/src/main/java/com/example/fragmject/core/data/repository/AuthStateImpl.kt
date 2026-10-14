package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.AuthState
import com.example.fragmject.core.domain.AuthStatus
import com.example.fragmject.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证状态实现：把 [UserRepository.observeCurrentUser] 映射为三态登录态。
 *
 * 首次读取本地用户之前保持 [AuthStatus.UNKNOWN]；读取失败按未登录处理，
 * 避免状态永久停留在未知而让受保护页面无法进入。
 *
 * 下沉到 core:data-repository（领域端口装配层），与 CollectState / ThemeState 归位方式一致；
 * app 层只依赖 core:domain 的 AuthState，不再 import UserRepository / User 模型。
 */
@Singleton
class AuthStateImpl @Inject constructor(
    userRepository: UserRepository,
    appScope: AppCoroutineScope,
) : AuthState {
    override val status: StateFlow<AuthStatus> =
        userRepository.observeCurrentUser()
            .map { if (it != null && it.id > 0) AuthStatus.LOGGED_IN else AuthStatus.LOGGED_OUT }
            .catch { emit(AuthStatus.LOGGED_OUT) }
            .stateIn(appScope, SharingStarted.Eagerly, AuthStatus.UNKNOWN)
}
