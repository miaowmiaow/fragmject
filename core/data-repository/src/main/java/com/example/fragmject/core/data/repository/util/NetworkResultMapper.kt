package com.example.fragmject.core.data.repository.util

import android.util.Log
import com.example.fragmject.core.data.contract.model.ApiCode
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.model.isSuccess
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.SimpleResult
import kotlinx.coroutines.CancellationException

/**
 * 服务端业务码 → 领域错误码的**唯一映射点**。
 *
 * - 空串（响应缺失业务码）→ [DomainResult.ERROR_UNKNOWN]；
 * - [ApiCode.LOGIN_REQUIRED] → [DomainResult.ERROR_UNAUTHENTICATED]；
 * - 其余原样透传，便于排查与后续按需扩展。
 */
internal fun HttpResponse.toFailureCode(): String = when (errorCode) {
    "" -> DomainResult.ERROR_UNKNOWN
    ApiCode.LOGIN_REQUIRED -> DomainResult.ERROR_UNAUTHENTICATED
    else -> errorCode
}

/**
 * 统一执行网络请求并映射为 [DomainResult]。
 *
 * 消除各 Repository 中重复的「runCatching + errorCode 判断 + 失败映射」样板：
 * - 网络调用抛异常（IO/超时等）→ Failure(ERROR_UNKNOWN)
 * - 业务失败（[isSuccess] 为 false）→ Failure([toFailureCode], 服务端消息)
 * - 业务成功 → 执行 [onSuccess] 并包装为 Success；映射/持久化失败同样返回 Failure
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

    return if (!resp.isSuccess) {
        DomainResult.Failure(
            code = resp.toFailureCode(),
            message = resp.errorMsg.ifBlank { "请求失败" },
        )
    } else {
        try {
            DomainResult.Success(onSuccess(resp))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("NetworkResultMapper", "onSuccess mapping failed", e)
            DomainResult.Failure(
                code = DomainResult.ERROR_UNKNOWN,
                message = "数据处理失败，请重试",
            )
        }
    }
}

/**
 * 统一执行**无数据写操作**（分享、收藏、下载）并映射为 [SimpleResult]。
 *
 * - 抛异常 → 失败，消息为 [failureMessage]（不向 UI 泄露底层异常文本）；
 * - 业务失败 → 失败，优先使用服务端消息，缺失时回退 [failureMessage]；
 * - 业务成功 → 成功，消息取服务端消息。
 *
 * [CancellationException] 原样向上抛出。
 */
suspend fun fetchAsSimpleResult(
    failureMessage: String,
    call: suspend () -> HttpResponse,
): SimpleResult {
    val resp = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("NetworkResultMapper", "call failed: $failureMessage", e)
        return SimpleResult(success = false, message = failureMessage)
    }
    return if (resp.isSuccess) {
        SimpleResult(success = true, message = resp.errorMsg)
    } else {
        SimpleResult(success = false, message = resp.errorMsg.ifBlank { failureMessage })
    }
}
