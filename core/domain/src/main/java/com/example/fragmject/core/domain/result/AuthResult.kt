package com.example.fragmject.core.domain.result

/**
 * 认证领域结果：登录 / 注册共用。
 *
 * 表达「成功 / 失败」，均携带可读消息。
 */
sealed class AuthResult {
    data class Success(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
