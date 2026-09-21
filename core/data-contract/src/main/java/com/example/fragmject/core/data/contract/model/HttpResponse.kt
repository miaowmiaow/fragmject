package com.example.fragmject.core.data.contract.model

/**
 * 通用 HTTP 响应基类（contract 层，供远端端口复用）。
 */
open class HttpResponse @JvmOverloads constructor(
    var errorCode: String = "",
    var errorMsg: String = ""
) {
    var time = System.currentTimeMillis()

    fun setRequestTime(reqTime: Long) {
        time -= reqTime
    }
}

/**
 * 带 data 载荷的 HTTP 响应容器。
 *
 * 与 [HttpResponse] 的区别：HttpResponse 只承载 errorCode/errorMsg，供无 data
 * 的接口（logout/collectArticle/shareArticle）复用；本类额外承载泛型 data 载荷。
 */
class DataResponse<T> @JvmOverloads constructor(
    var data: T? = null,
    errorCode: String = "",
    errorMsg: String = "",
) : HttpResponse(errorCode, errorMsg)
