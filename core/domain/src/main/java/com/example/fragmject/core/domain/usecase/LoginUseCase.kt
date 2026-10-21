package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.result.AuthResult
import javax.inject.Inject

/**
 * 登录：参数校验 + 会话编排。
 *
 * 职责边界：transport 交给 [AuthGateway]，会话一致性（快照/回滚/持久化）交给
 * [AuthSessionCoordinator]；本类只表达「登录」这个用例本身。
 */
class LoginUseCase @Inject constructor(
    private val gateway: AuthGateway,
    private val session: AuthSessionCoordinator,
) {
    suspend operator fun invoke(username: String, password: String): AuthResult {
        if (username.isBlank()) return AuthResult.Error("用户名不能为空")
        if (password.isBlank()) return AuthResult.Error("密码不能为空")
        return session.authenticate { gateway.login(username, password) }
    }
}
