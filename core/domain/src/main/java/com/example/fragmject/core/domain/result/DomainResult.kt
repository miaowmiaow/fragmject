package com.example.fragmject.core.domain.result

/**
 * 领域层通用结果包装：**带数据的查询 / 刷新**统一用它表达「成功 / 失败」。
 *
 * 结果类型约定（新增端口请按此选型，不要再造新的结果类型）：
 * - 带数据的查询、刷新：[DomainResult]；
 * - 无数据的写操作（分享、收藏、下载）：[SimpleResult]；
 * - 登录 / 注册：[AuthResult]（需要把认证消息交给 UI）；
 * - 媒体保存：[MediaSaveResult]（需要回传路径与 uri）。
 * 以上类型都只表达**预期失败**；协程取消必须原样抛出，不得转成失败结果。
 *
 * 失败携带错误码与可读消息；错误码取值见 [Failure.code]。
 * 加载中（Loading）状态由 ViewModel 自行管理。
 */
sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>

    /**
     * @param code 领域错误码：[ERROR_UNKNOWN]、[ERROR_UNAUTHENTICATED]，
     *   其余为服务端返回的业务码原值（data 层不识别时原样透传）。
     */
    data class Failure(
        val code: String,
        val message: String,
    ) : DomainResult<Nothing> {
        /** 服务端要求登录（未登录或会话过期）。 */
        val isUnauthenticated: Boolean get() = code == ERROR_UNAUTHENTICATED
    }

    companion object {
        const val ERROR_UNKNOWN = "UNKNOWN_ERROR"

        /** 需要登录：由 data 层把服务端的「要求登录」业务码统一映射为此值。 */
        const val ERROR_UNAUTHENTICATED = "UNAUTHENTICATED"
    }
}

/** 成功时返回数据，失败返回 `null`；仅用于「失败可以安全降级为无数据」的读取。 */
fun <T> DomainResult<T>.getOrNull(): T? = (this as? DomainResult.Success)?.data
