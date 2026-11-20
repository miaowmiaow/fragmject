package com.example.fragmject.core.network.http

import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 凭据提交端点的识别口径。
 *
 * 这层判定的用途是「对登录 / 注册额外屏蔽响应体与 URL query」；请求体本身是**无条件**
 * 不打印的，因此即使这里漏判，密码也不会进日志——这是与「按路径白名单保护请求体」的关键区别。
 */
class DebugHttpLoggerTest {

    private fun url(path: String) = Request.Builder()
        .url("https://www.wanandroid.com$path")
        .build()
        .url

    @Test
    fun `post to credential endpoints is treated as sensitive`() {
        listOf(
            "/user/login",
            "/user/login/",
            "/user/login/json",
            "/user/register",
            "/user/register/json",
        ).forEach { path ->
            assertTrue(path, isCredentialSubmission("POST", url(path)))
        }
    }

    @Test
    fun `path matching is segment based and case insensitive`() {
        assertTrue(isCredentialSubmission("post", url("/USER/LOGIN")))
        // 不能按前缀硬匹配：user/loginHistory 这类同级路径不是凭据提交
        assertFalse(isCredentialSubmission("POST", url("/user/loginHistory")))
        assertFalse(isCredentialSubmission("POST", url("/user/login2")))
    }

    @Test
    fun `read only endpoints are not sensitive`() {
        listOf("/article/list/0/json", "/banner/json", "/user/logout/json", "/user/center/json")
            .forEach { path -> assertFalse(path, isCredentialSubmission("GET", url(path))) }
        // 形状相似但不是凭据提交的 POST 也不在屏蔽范围（响应体仍可调试）
        assertFalse(isCredentialSubmission("POST", url("/lg/collect/1/json")))
    }

    @Test
    fun `only body carrying methods count as credential submission`() {
        listOf("GET", "HEAD", "DELETE").forEach { method ->
            assertFalse(method, isCredentialSubmission(method, url("/user/login")))
        }
        assertTrue(isCredentialSubmission("post", url("/user/login")))
    }
}
