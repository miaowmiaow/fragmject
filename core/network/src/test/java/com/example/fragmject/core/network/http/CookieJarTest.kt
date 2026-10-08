package com.example.fragmject.core.network.http

import com.example.fragmject.core.domain.session.CookieStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [CookieJar] 测试：验证 OkHttp 与 CookieStore 之间的桥接行为。
 *
 * 此前 CookieJar 内部直接静态调用平台 CookieStore，无法替换；
 * 改为注入端口后可用 fake 验证解析与写入。
 */
class CookieJarTest {

    private class FakeCookieStore : CookieStore {
        val written = LinkedHashMap<String, String>()
        var flushed = 0
        private val stored = mutableMapOf<String, String>()

        override fun getCookie(url: String): String? = stored[url]

        override fun setCookie(url: String, value: String) {
            // 模拟 CookieManager.setCookie 的累加语义：同一 url 多次写入应累加而非覆盖
            written[url] = written[url]?.let { "$it; $value" } ?: value
            stored[url] = value
        }

        override fun flush() {
            flushed++
        }
    }

    private val url = "https://www.wanandroid.com/user/login".toHttpUrl()

    @Test
    fun `loadForRequest parses cookies split by semicolon`() {
        val store = FakeCookieStore()
        store.setCookie(url.toString(), "a=1; b=2")
        val jar = CookieJar(store)

        val cookies = jar.loadForRequest(url)

        assertEquals(2, cookies.size)
        assertEquals("a", cookies[0].name)
        assertEquals("1", cookies[0].value)
        assertEquals("b", cookies[1].name)
    }

    @Test
    fun `loadForRequest returns empty when no cookie`() {
        val jar = CookieJar(FakeCookieStore())

        assertTrue(jar.loadForRequest(url).isEmpty())
    }

    @Test
    fun `saveFromResponse writes each cookie and flushes once`() {
        val store = FakeCookieStore()
        val jar = CookieJar(store)
        val cookies = listOf(
            okhttp3.Cookie.Builder().name("a").value("1").domain("www.wanandroid.com").build(),
            okhttp3.Cookie.Builder().name("b").value("2").domain("www.wanandroid.com").build(),
        )

        jar.saveFromResponse(url, cookies)

        assertEquals(1, store.flushed)
        assertTrue(store.written.values.any { it.contains("a=1") })
        assertTrue(store.written.values.any { it.contains("b=2") })
    }
}
