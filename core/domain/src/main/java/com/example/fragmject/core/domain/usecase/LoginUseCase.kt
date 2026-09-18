package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.LoginResult
import javax.inject.Inject

/**
 * 封装登录逻辑：验证 → 调用领域端口。
 * 登录成功后由 data 层 Adapter 持久化用户。
 */
class LoginUseCase @Inject constructor(
    private val userRepo: UserRepository,
) {
    suspend operator fun invoke(username: String, password: String): LoginResult {
        if (username.isBlank()) return LoginResult.Error("用户名不能为空")
        if (password.isBlank()) return LoginResult.Error("密码不能为空")
        return userRepo.login(username, password)
    }
}