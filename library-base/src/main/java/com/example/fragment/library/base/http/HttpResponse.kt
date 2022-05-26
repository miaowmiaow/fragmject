package com.example.fragment.library.base.http

open class HttpResponse @JvmOverloads constructor(
    var errorCode: String = "",
    var errorMsg: String = ""
) {
    var time = 0L

    init {
        time = System.currentTimeMillis()
    }
}