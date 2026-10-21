package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.result.AuthResult
import javax.inject.Inject

/**
 * 注册：参数校验 + 会话编排。
 *
 * 与 [LoginUseCase] 共用 [AuthSessionCoordinator]，保证「认证成功后落库、失败后回滚」的
 * 规则只有一份实现。
 */
class RegisterUseCase @Inject constructor(
    private val gateway: AuthGateway,
    private val session: AuthSessionCoordinator,
) {
    suspend operator fun invoke(
        username: String,
        password: String,
        repassword: String,
    ): AuthResult {
        if (username.isBlank()) return AuthResult.Error("用户名不能为空")
        if (password.isBlank()) return AuthResult.Error("密码不能为空")
        if (repassword.isBlank()) return AuthResult.Error("确认密码不能为空")
        if (password != repassword) return AuthResult.Error("两次密码不一样")
        return session.authenticate { gateway.register(username, password, repassword) }
    }
}
