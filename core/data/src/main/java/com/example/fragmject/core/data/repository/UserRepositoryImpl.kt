package com.example.fragmject.core.data.repository

import com.example.fragmject.core.network.datasource.UserRemoteDataSource

import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.store.UserStore
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.LoginResult
import com.example.fragmject.core.domain.result.LogoutResult
import com.example.fragmject.core.domain.result.RegisterResult
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UserRepository] 领域端口的 data 层适配器。
 *
 * 消化 [UserRemoteDataSource] 返回的 DataResponse，转换为领域结果类型，
 * 并在 login/register 成功后持久化用户、logout 成功后清空会话。
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val userStore: UserStore,
) : UserRepository {

    override suspend fun login(username: String, password: String): LoginResult {
        val resp = remote.login(username, password)
        resp.data?.let { saveUser(it) }
        return if (resp.errorCode == "0") {
            LoginResult.Success(resp.errorMsg)
        } else {
            LoginResult.Error(resp.errorMsg)
        }
    }

    override suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): RegisterResult {
        val resp = remote.register(username, password, repassword)
        resp.data?.let { saveUser(it) }
        return if (resp.errorCode == "0") {
            RegisterResult.Success(resp.errorMsg)
        } else {
            RegisterResult.Error(resp.errorMsg)
        }
    }

    override suspend fun logout(): LogoutResult {
        val resp = remote.logout()
        if (resp.errorCode == "0") {
            userStore.clearUser()
        }
        return LogoutResult(success = resp.errorCode == "0")
    }

    override suspend fun saveUser(user: User) {
        userStore.setUser(user)
    }

    override fun observeCurrentUser(): Flow<User?> =
        userStore.getUser().map { it?.toDomain() }
}
