package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户/账号领域端口：登录 / 注册 / 登出 / 会话持久化。
 *
 * 实现由 data 层适配器（Adapter）提供，内部消化网络响应并通过
 * [com.example.fragmject.core.data.contract.local.UserLocalDataSource] 持久化会话。
 * login/register 成功后由 Adapter 持久化用户，显式 logout 总是尝试清空本地用户与业务 Cookie。
 */
interface UserRepository {
    suspend fun login(username: String, password: String): AuthResult
    suspend fun register(username: String, password: String, repassword: String): AuthResult
    /**
     * 总是尝试清除本地会话，清理完成返回 true；远端退出是 best effort。
     * 本地清理失败抛出异常；调用取消仍传播，但不跳过本地清理。
     */
    suspend fun logout(): Boolean

    /** 持久化当前登录用户。 */
    suspend fun saveUser(user: User)

    /** 观察当前登录用户（未登录时为 null）。 */
    fun observeCurrentUser(): Flow<User?>
}
