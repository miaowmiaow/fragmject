package com.example.fragmject.core.domain.result

/** 登录结果。 */
sealed class LoginResult {
    data class Success(val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

/** 注册结果。 */
sealed class RegisterResult {
    data class Success(val message: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}

/** 登出结果。 */
data class LogoutResult(
    val success: Boolean,
)
