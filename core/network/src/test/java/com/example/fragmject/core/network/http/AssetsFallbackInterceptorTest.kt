package com.example.fragmject.core.network.http

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 兜底判定逻辑的单测：只覆盖「什么响应该被替换」，不涉及 assets 读取与拦截器链。
 */
class AssetsFallbackInterceptorTest {

    @Test
    fun `http failure needs fallback`() {
        assertTrue(
            response(code = 500, body = "<html>500</html>").needsAssetsFallback()
        )
    }

    @Test
    fun `server side query failure needs fallback`() {
        // 玩Android 数据库故障时的真实返回：HTTP 200，业务码 -1
        assertTrue(
            response(
                code = 200,
                body = """{"data":null,"errorCode":-1,"errorMsg":"could not execute query"}""",
            ).needsAssetsFallback()
        )
    }

    @Test
    fun `success business code does not need fallback`() {
        assertFalse(
            response(
                code = 200,
                body = """{"data":{"datas":[],"over":true},"errorCode":0,"errorMsg":""}""",
            ).needsAssetsFallback()
        )
    }

    @Test
    fun `missing business code is left to downstream`() {
        assertFalse(response(code = 200, body = """{"data":[]}""").needsAssetsFallback())
    }

    @Test
    fun `unparsable body is left to downstream`() {
        assertFalse(response(code = 200, body = "<html></html>").needsAssetsFallback())
    }

    private fun response(code: Int, body: String): Response = Response.Builder()
        .request(Request.Builder().url("https://www.wanandroid.com/article/list/0/json").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("OK")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()
}
