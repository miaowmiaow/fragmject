package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UserRepository] 领域端口的 data 层适配器。
 *
 * 依赖 data-contract 的 [UserRemoteDataSource] 与 [UserLocalDataSource]，
 * 消化网络响应、转换为领域结果，并在 login/register 成功后持久化用户、
 * logout 成功后清空会话。不感知 Retrofit / Room。
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val local: UserLocalDataSource,
) : UserRepository {

    override suspend fun login(username: String, password: String): AuthResult {
        val resp = remote.login(username, password)
        return if (resp.errorCode == "0") {
            val user = resp.data
            if (user == null) {
                AuthResult.Error("用户信息获取失败")
            } else {
                saveUser(user)
                AuthResult.Success(resp.errorMsg)
            }
        } else {
            AuthResult.Error(resp.errorMsg)
        }
    }

    override suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): AuthResult {
        val resp = remote.register(username, password, repassword)
        return if (resp.errorCode == "0") {
            val user = resp.data
            if (user == null) {
                AuthResult.Error("用户信息获取失败")
            } else {
                saveUser(user)
                AuthResult.Success(resp.errorMsg)
            }
        } else {
            AuthResult.Error(resp.errorMsg)
        }
    }

    override suspend fun logout(): Boolean {
        val resp = remote.logout()
        if (resp.errorCode == "0") {
            local.clearUser()
        }
        return resp.errorCode == "0"
    }

    override suspend fun saveUser(user: User) {
        local.saveUser(user)
    }

    override fun observeCurrentUser(): Flow<User?> = local.observeCurrentUser()
}