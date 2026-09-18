package com.example.fragmject.core.network.http

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
 * 替代此前散落在 core:model 中的 XxxList / Login / Register 等响应包装类，
 * 让响应 DTO 回归 core:network，而 core:model 只保留纯领域实体。
 *
 * 与 [HttpResponse] 的区别：HttpResponse 只承载 errorCode/errorMsg，供无 data
 * 的接口（logout/collectArticle/shareArticle）复用；本类额外承载泛型 data 载荷。
 */
class DataResponse<T> @JvmOverloads constructor(
    var data: T? = null,
    errorCode: String = "",
    errorMsg: String = "",
) : HttpResponse(errorCode, errorMsg)