package com.example.fragmject.core.network.http

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * 网络层 Gson 单例。
 *
 * 注册 [NullSafeStringTypeAdapterFactory]：服务端响应字段可能缺失，缺失的非空
 * String 字段回填空串，避免 `ifBlank`/`hashCode` 等对 null 的 NPE。
 * 供 Retrofit 的 `GsonConverterFactory` 与网络层反序列化统一复用。
 */
object GsonUtils {

    /** 网络反序列化专用 Gson 单例：缺失的非空 String 回填空串。 */
    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapterFactory(NullSafeStringTypeAdapterFactory)
            .create()
    }
}
