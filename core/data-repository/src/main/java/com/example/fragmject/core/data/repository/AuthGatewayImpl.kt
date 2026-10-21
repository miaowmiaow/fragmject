package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.isSuccess
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AuthGateway] 的 data 层适配器：只做 transport + 响应翻译。
 *
 * 刻意不写库、不清 Cookie、不持会话锁 —— 这些是会话编排
 * （`core:domain` 的 `AuthSessionCoordinator`）的职责；
 * 网络异常与取消原样上抛，由应用层决定提示与回滚顺序。
 */
@Singleton
class AuthGatewayImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
) : AuthGateway {

    override suspend fun login(username: String, password: String): AuthExchange =
        remote.login(username, password).toExchange()

    override suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): AuthExchange = remote.register(username, password, repassword).toExchange()

    override suspend fun logout() {
        // 返回值只表达远端是否配合退出，调用方按 best effort 处理，故此处忽略
        remote.logout()
    }

    private fun DataResponse<User>.toExchange(): AuthExchange {
        val user = data
        return when {
            // 业务失败：即使响应体里带了 user 也不能落库
            !isSuccess -> AuthExchange.Failure(errorMsg)
            // HTTP 成功却没有用户体：会话可能已建立，标记为需要无条件清理
            user == null -> AuthExchange.Failure("用户信息获取失败", sessionTouched = true)
            else -> AuthExchange.Success(user, errorMsg)
        }
    }
}
