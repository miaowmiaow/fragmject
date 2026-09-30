package com.example.fragmject.core.data.contract.model

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
