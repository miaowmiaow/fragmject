package com.example.fragmject.core.domain.result

/**
 * 领域层通用结果包装。
 *
 * 用于表达「成功 / 失败」，失败携带业务错误码与可读消息。
 * 加载中（Loading）状态由 ViewModel 自行管理。
 */
sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(
        val code: String,
        val message: String,
    ) : DomainResult<Nothing>

    companion object {
        const val ERROR_UNKNOWN = "UNKNOWN_ERROR"
    }
}
