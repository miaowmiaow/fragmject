package com.example.fragmject.core.domain.result

/** 注册结果。 */
sealed class RegisterResult {
    data class Success(val message: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}
