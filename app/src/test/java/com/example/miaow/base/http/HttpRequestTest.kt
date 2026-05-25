package com.example.miaow.base.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [HttpRequest] 路径替换 / Query 拼接的纯 JVM 单测。
 *
 * 不依赖 Android Framework，可在 `./gradlew :app:testDebugUnitTest` 中直接运行。
 *
 * 这一组用例同时回归之前修复过的"`getUrl()` 反复调用会累计副作用"的问题：
 * 多次调用 [HttpRequest.getUrl] 必须得到完全相同的结果。
 */
class HttpRequestTest {

    @Test
    fun `getUrl replaces path placeholders`() {
        val req = HttpRequest("article/{cid}/json")
            .putPath("cid", "60")
        assertEquals("article/60/json", req.getUrl())
    }

    @Test
    fun `getUrl appends query parameters with question mark`() {
        val req = HttpRequest("article/list")
            .putQuery("page", "0")
            .putQuery("size", "20")
        val url = req.getUrl()
        // query 顺序在 LinkedHashMap-like 行为下是确定的；但为兼容实现细节
        // 这里只校验关键事实：以 ? 起始、以 & 拼接、不以 & 结尾
        assertTrue("url should start with path and ?: $url", url.startsWith("article/list?"))
        assertTrue("page should be present: $url", url.contains("page=0"))
        assertTrue("size should be present: $url", url.contains("size=20"))
        assertTrue("trailing & should be trimmed: $url", !url.endsWith("&"))
    }

    @Test
    fun `getUrl is idempotent across multiple invocations`() {
        // 历史 Bug：getUrl() 内部修改成员变量 url，反复调用会累计 path 替换。
        // 修复后必须保证多次调用结果完全一致。
        val req = HttpRequest("user/{userId}/coin")
            .putPath("userId", "1024")
            .putQuery("page", "1")
        val first = req.getUrl()
        val second = req.getUrl()
        val third = req.getUrl()
        assertEquals(first, second)
        assertEquals(second, third)
    }

    @Test
    fun `putPath ignores empty value`() {
        val req = HttpRequest("article/{cid}/json")
            .putPath("cid", "")
        // 空值不会写入 path map，因此占位符不会被替换
        assertEquals("article/{cid}/json", req.getUrl())
    }

    @Test
    fun `getUrl without query keeps original path untouched`() {
        val req = HttpRequest("banner/json")
        assertEquals("banner/json", req.getUrl())
    }
}
