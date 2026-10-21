package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 当前用户存储端口：只管「本地那条用户记录」，不承担认证与会话策略。
 *
 * 认证请求走 [com.example.fragmject.core.domain.auth.AuthGateway]，
 * 会话一致性（Cookie 快照/回滚 + 落库时机）走
 * [com.example.fragmject.core.domain.usecase.AuthSessionCoordinator]；
 * 本端口仅剩 observe / save / clear 三个原子操作，实现见
 * `core:data-repository` 的 `UserRepositoryImpl`（依赖 `UserLocalDataSource`）。
 */
interface UserRepository {

    /** 观察当前登录用户（未登录时为 null）。 */
    fun observeCurrentUser(): Flow<User?>

    /** 持久化当前登录用户。 */
    suspend fun saveUser(user: User)

    /** 清除本地用户记录；业务 Cookie 的清理由 `CookieStore` 负责。 */
    suspend fun clearUser()
}
