package com.example.fragmject.core.data

import com.example.fragmject.core.common.result.AppError
import com.example.fragmject.core.common.result.AppResult
import com.example.fragmject.core.common.result.toAppError
import com.example.fragmject.core.network.http.HttpResponse

/**
 * HTTP 响应 → AppResult 转换工具。
 *
 * 从 [com.example.fragmject.core.common.result] 移入 :data，
 * 因为这里的逻辑直接依赖 [HttpResponse]，属于数据层专属。
 */
fun <T : HttpResponse> T.toAppResult(): AppResult<T> {
    return if (errorCode == AppError.SUCCESS) {
        AppResult.Success(this)
    } else {
        AppResult.Failure(
            AppError.Business(
                code = errorCode.ifBlank { AppError.ERROR_UNKNOWN },
                message = errorMsg.ifBlank { "请求失败" },
            )
        )
    }
}

/**
 * 专用于 [HttpResponse] 子类的安全执行包装。
 * 成功时额外检查 [HttpResponse.errorCode]，非 "0" 时转为 [AppError.Business]。
 */
suspend inline fun <reified T : HttpResponse> runHttpResult(crossinline block: suspend () -> T): AppResult<T> {
    return kotlin.runCatching { block() }
        .fold(
            onSuccess = { it.toAppResult() },
            onFailure = { AppResult.Failure(it.toAppError()) },
        )
}
