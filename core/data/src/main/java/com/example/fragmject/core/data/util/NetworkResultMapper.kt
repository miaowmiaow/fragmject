package com.example.fragmject.core.data.util

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.network.http.DataResponse
import kotlinx.coroutines.CancellationException

/**
 * 统一执行网络请求并映射为 [DomainResult]。
 *
 * 消除各 Repository 中重复的「runCatching + errorCode 判断 + 失败映射」样板：
 * - 网络调用抛异常（IO/超时等）→ Failure(ERROR_UNKNOWN)
 * - errorCode != "0" → Failure(业务错误码)
 * - errorCode == "0" → 执行 [onSuccess] 并包装为 Success
 *
 * [CancellationException] 会原样向上抛出，避免吞掉协程取消信号（区别于 runCatching 的全量捕获）。
 */
suspend fun <T, R> fetchAsDomainResult(
    call: suspend () -> DataResponse<T>,
    onSuccess: suspend (DataResponse<T>) -> R,
): DomainResult<R> {
    val resp = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return DomainResult.Failure(
            code = DomainResult.ERROR_UNKNOWN,
            message = e.message ?: "请求失败",
        )
    }

    return if (resp.errorCode != "0") {
        DomainResult.Failure(
            code = resp.errorCode.ifBlank { DomainResult.ERROR_UNKNOWN },
            message = resp.errorMsg.ifBlank { "请求失败" },
        )
    } else {
        DomainResult.Success(onSuccess(resp))
    }
}
