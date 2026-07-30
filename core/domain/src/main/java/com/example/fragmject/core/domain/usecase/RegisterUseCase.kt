package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.UserRepository
import com.example.fragmject.core.database.store.UserStore
import javax.inject.Inject

/**
 * 封装注册逻辑：验证 → 调用 API → 持久化用户。
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

        val response = userRepo.register(username, password, repassword)
        response.data?.let { UserStore.setUser(it) }
        return if (response.errorCode == "0") {
            RegisterResult.Success(response.errorMsg)
        } else {
            RegisterResult.Error(response.errorMsg)
        }
    }
}

sealed class RegisterResult {
    data class Success(val message: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}