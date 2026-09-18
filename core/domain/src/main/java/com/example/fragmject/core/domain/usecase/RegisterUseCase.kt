package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.RegisterResult
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
    ): RegisterResult {
        if (username.isBlank()) return RegisterResult.Error("用户名不能为空")
        if (password.isBlank()) return RegisterResult.Error("密码不能为空")
        if (repassword.isBlank()) return RegisterResult.Error("确认密码不能为空")
        if (password != repassword) return RegisterResult.Error("两次密码不一样")
        return userRepo.register(username, password, repassword)
    }
}