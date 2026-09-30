package com.example.fragmject.core.domain.result

/** 登录结果。 */
sealed class LoginResult {
    data class Success(val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

