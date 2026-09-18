package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.LoginResult
import com.example.fragmject.core.domain.result.LogoutResult
import com.example.fragmject.core.domain.result.RegisterResult
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户/账号领域端口：登录 / 注册 / 登出 / 会话持久化。
 *
 * 实现由 data 层适配器（Adapter）提供，内部消化网络响应并处理 [UserStore]。
 * login/register 成功后由 Adapter 持久化用户，logout 成功后由 Adapter 清空会话。
 */
interface UserRepository {
    suspend fun login(username: String, password: String): LoginResult
    suspend fun register(username: String, password: String, repassword: String): RegisterResult
    suspend fun logout(): LogoutResult

    /** 持久化当前登录用户。 */
    suspend fun saveUser(user: User)

    /** 观察当前登录用户（未登录时为 null）。 */
    fun observeCurrentUser(): Flow<User?>
}
