package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.UserRepository
import com.example.fragmject.core.database.store.UserStore
import javax.inject.Inject

/**
 * 封装登录逻辑：验证 → 调用 API → 持久化用户。
 * ViewModel 只需根据 [LoginResult] 设置 UI 状态。
 */
class LoginUseCase @Inject constructor(
    private val userRepo: UserRepository,
) {
    suspend operator fun invoke(username: String, password: String): LoginResult {
        if (username.isBlank()) return LoginResult.Error("用户名不能为空")
        if (password.isBlank()) return LoginResult.Error("密码不能为空")

        val response = userRepo.login(username, password)
        response.data?.let { UserStore.setUser(it) }
        return if (response.errorCode == "0") {
            LoginResult.Success(response.errorMsg)
        } else {
            LoginResult.Error(response.errorMsg)
        }
    }
}

/** 登录结果，只需区分成功和错误两种状态（Loading 由 ViewModel 自行管理）。 */
sealed class LoginResult {
    data class Success(val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}