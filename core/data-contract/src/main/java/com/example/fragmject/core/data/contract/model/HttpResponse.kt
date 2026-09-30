package com.example.fragmject.core.data.contract.model

/**
 * 通用 HTTP 响应基类（contract 层，供远端端口复用）。
 *
 * 只承载 errorCode/errorMsg，供无 data 的接口（logout/collectArticle/shareArticle）复用。
 * 带 data 载荷的场景见 [DataResponse]。
 */
open class HttpResponse @JvmOverloads constructor(
    var errorCode: String = "",
    var errorMsg: String = ""
)
