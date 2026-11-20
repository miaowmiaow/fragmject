package com.example.fragmject.core.network

import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 缓存决策响应头的提取口径。
 *
 * 核心不变量：`Cache-Control` / `Vary` **多行必须全量合并**。OkHttp 的 `header(name)`
 * 只返回最后一个值，若服务端把 `no-store` 写在前面、`max-age` 写在后面，
 * 单值读取会让「不可缓存」的响应被写进磁盘。
 */
class CacheRelevantHeadersTest {

    @Test
    fun `split cache-control lines are merged instead of last-one-wins`() {
        val headers = Headers.Builder()
            .add("Cache-Control", "no-store")
            .add("Cache-Control", "max-age=600")
            .build()

        val mapped = headers.cacheRelevantHeaders()

        assertEquals("no-store, max-age=600", mapped["Cache-Control"])
    }

    @Test
    fun `split vary lines are merged so cookie dimension is not lost`() {
        val headers = Headers.Builder()
            .add("Vary", "Cookie")
            .add("Vary", "Accept-Encoding")
            .build()

        val mapped = headers.cacheRelevantHeaders()

        assertEquals("Cookie, Accept-Encoding", mapped["Vary"])
    }

    @Test
    fun `merged values keep the directives the cache gate looks for`() {
        val mapped = Headers.Builder()
            .add("Cache-Control", "public")
            .add("Cache-Control", "private")
            .build()
            .cacheRelevantHeaders()

        val directives = mapped.getValue("Cache-Control")
            .split(',')
            .map { it.trim() }
        assertTrue("private 不能因多行而丢失", directives.contains("private"))
    }

    @Test
    fun `content type stays single valued`() {
        val headers = Headers.Builder()
            .add("Content-Type", "text/html; charset=utf-8")
            .build()

        val mapped = headers.cacheRelevantHeaders()

        assertEquals("text/html; charset=utf-8", mapped["Content-Type"])
    }

    @Test
    fun `headers that are absent are not reported`() {
        val mapped = Headers.Builder().build().cacheRelevantHeaders()

        assertTrue(mapped.isEmpty())
        assertNull(mapped["Cache-Control"])
        assertNull(mapped["Vary"])
        assertNull(mapped["Content-Type"])
    }

    @Test
    fun `header names are matched case insensitively`() {
        val headers = Headers.Builder()
            .add("cache-control", "no-store")
            .add("vary", "cookie")
            .build()

        val mapped = headers.cacheRelevantHeaders()

        // OkHttp 的 Headers 本身大小写不敏感，这里锁住「取值时用的名字不会漏」
        assertEquals("no-store", mapped["Cache-Control"])
        assertEquals("cookie", mapped["Vary"])
    }
}
