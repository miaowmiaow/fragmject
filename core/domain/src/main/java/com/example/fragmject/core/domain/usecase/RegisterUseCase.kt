package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import javax.inject.Inject

/**
 * 封装注册逻辑：验证 → 调用领域端口。
 * 注册成功后由 data 层 Adapter 持久化用户。
 */
class RegisterUseCase @Inject constructor(
    private val userRepo: UserRepository,
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
        return userRepo.register(username, password, repassword)
    }
}