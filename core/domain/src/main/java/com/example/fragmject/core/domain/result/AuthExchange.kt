package com.example.fragmject.core.domain.result

import com.example.fragmject.core.model.User

/**
 * 认证交换结果：数据层只回答「这一次认证请求换回了什么」，不产生任何会话副作用。
 *
 * 会话清理、快照与用户持久化由应用层
 * [com.example.fragmject.core.domain.usecase.AuthSessionCoordinator] 编排，
 * 数据层只负责 transport 与 storage 两类原子操作。
 */
sealed interface AuthExchange {

    /** 认证成功：携带服务端用户与提示文案。 */
    data class Success(
        val user: User,
        val message: String,
    ) : AuthExchange

    /**
     * 认证失败。
     *
     * [sessionTouched] 表达「这次失败的请求是否可能已经改写了业务 Cookie」：
     * - `false`（HTTP/业务失败）：调用方按「Cookie 是否真的变了」决定要不要回滚；
     * - `true`（HTTP 成功但缺用户体）：会话可能已建立，必须无条件清理。
     */
    data class Failure(
        val message: String,
        val sessionTouched: Boolean = false,
    ) : AuthExchange
}
