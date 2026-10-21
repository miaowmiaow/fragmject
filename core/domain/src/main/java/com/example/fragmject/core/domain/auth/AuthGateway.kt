package com.example.fragmject.core.domain.auth

import com.example.fragmject.core.domain.result.AuthExchange

/**
 * 认证传输端口：只做「发请求 + 把响应翻译成 [AuthExchange]」。
 *
 * 明确不做的事（这些属于应用层编排，见
 * [com.example.fragmject.core.domain.usecase.AuthSessionCoordinator]）：
 * - 不写本地用户库、不清 Cookie、不持会话锁；
 * - 不把异常翻译成用户文案（网络异常原样上抛，由应用层决定提示与清理）。
 *
 * 实现归属：`core:data-repository` 的 `AuthGatewayImpl`（依赖 `UserRemoteDataSource`）。
 */
interface AuthGateway {

    suspend fun login(username: String, password: String): AuthExchange

    suspend fun register(username: String, password: String, repassword: String): AuthExchange

    /**
     * 远端登出。失败以异常抛出（含取消），由 [com.example.fragmject.core.domain.usecase.LogoutUseCase]
     * 决定 best effort 的清理顺序与异常优先级。
     */
    suspend fun logout()
}
