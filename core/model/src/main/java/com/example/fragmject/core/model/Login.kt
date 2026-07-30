package com.example.fragmject.core.model

import com.example.fragmject.core.network.http.HttpResponse

data class Login(
    val data: User? = null
) : HttpResponse()

data class Register(
    val data: User? = null
) : HttpResponse()