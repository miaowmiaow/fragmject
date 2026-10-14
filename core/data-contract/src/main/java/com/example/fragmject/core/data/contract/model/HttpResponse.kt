package com.example.fragmject.core.data.contract.model

/**
 * 通用 HTTP 响应基类（contract 层，供远端端口复用）。
 *
 * 只承载 errorCode/errorMsg，供无 data 的接口（logout/collectArticle/shareArticle）复用。
 * 带 data 载荷的场景见 [DataResponse]。
 *
 * 成功与否一律经 [isSuccess] 判断，业务码常量见 [ApiCode]，禁止在调用方散落 `"0"` 字面量。
 */
open class HttpResponse @JvmOverloads constructor(
    var errorCode: String = "",
    var errorMsg: String = ""
)

/**
 * 服务端业务码与本地约定码的唯一来源。
 *
 * 服务端（玩Android）用 `errorCode` 字符串表达业务结果；
 * 本地在网络异常、非 2xx 等无法拿到服务端业务码时，也复用该字段表达失败。
 */
object ApiCode {
    /** 业务成功。 */
    const val SUCCESS = "0"

    /** 本地生成的通用失败：网络/IO 异常、HTTP 非 2xx 等。 */
    const val CLIENT_FAILURE = "-1"

    /** 服务端要求登录：未登录或会话已过期。 */
    const val LOGIN_REQUIRED = "-1001"
}

/** 业务是否成功：`errorCode` 必须严格等于 [ApiCode.SUCCESS]，空串与缺失一律视为失败。 */
val HttpResponse.isSuccess: Boolean get() = errorCode == ApiCode.SUCCESS
