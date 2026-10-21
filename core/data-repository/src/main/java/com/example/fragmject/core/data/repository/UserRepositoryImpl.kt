package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * [UserRepository] 的 data 层适配器：当前用户的本地存储。
 *
 * 只做三件事：观察、保存、清除。认证传输见 [AuthGatewayImpl]，
 * 会话一致性（Cookie 快照/回滚、落库时机、登出策略）见
 * `core:domain` 的 `AuthSessionCoordinator` —— data 层不再承载业务编排。
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val local: UserLocalDataSource,
) : UserRepository {

    override fun observeCurrentUser(): Flow<User?> = local.observeCurrentUser()

    override suspend fun saveUser(user: User) {
        local.saveUser(user)
    }

    override suspend fun clearUser() {
        local.clearUser()
    }
}
