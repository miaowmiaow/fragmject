package com.example.fragmject.core.network.http

import com.example.fragmject.core.domain.session.CookieStore
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
        var failOnName: String? = null
        var writeFailure = IOException("write failed")
        var flushFailure: Exception? = null
        private val stored = mutableMapOf<String, String>()

        override fun getCookie(url: String): String? = stored[url]

        override fun setCookie(url: String, value: String) {
            if (value.substringBefore('=') == failOnName) throw writeFailure
            // 模拟 CookieManager.setCookie 的累加语义：同一 url 多次写入应累加而非覆盖
            written[url] = written[url]?.let { "$it; $value" } ?: value
            stored[url] = value
        }

        override fun flush() {
            flushed++
            flushFailure?.let { throw it }
        }

        override suspend fun clearSession() {
            stored.keys.removeAll { it.toHttpUrl().host in setOf("wanandroid.com", "www.wanandroid.com") }
            flush()
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

    @Test
    fun `saveFromResponse with no cookies does not flush`() {
        val store = FakeCookieStore()

        CookieJar(store).saveFromResponse(url, emptyList())

        assertEquals(0, store.flushed)
    }

    @Test
    fun `partial write failure still flushes completed writes`() {
        val store = FakeCookieStore().apply { failOnName = "b" }
        val cookies = listOf(
            okhttp3.Cookie.Builder().name("a").value("1").domain(url.host).build(),
            okhttp3.Cookie.Builder().name("b").value("2").domain(url.host).build(),
        )
        try {
            CookieJar(store).saveFromResponse(url, cookies)
            fail("Write failure expected")
        } catch (e: IOException) {
            assertSame(store.writeFailure, e)
        }
        assertEquals(1, store.flushed)
        assertTrue(store.written.values.any { it.contains("a=1") })
        assertTrue(store.written.values.none { it.contains("b=2") })
    }

    @Test
    fun `flush failure after write failure preserves original exception`() {
        val persistenceFailure = IOException("flush failed")
        val store = FakeCookieStore().apply {
            failOnName = "a"
            flushFailure = persistenceFailure
        }
        val cookie = okhttp3.Cookie.Builder().name("a").value("1").domain(url.host).build()
        try {
            CookieJar(store).saveFromResponse(url, listOf(cookie))
            fail("Write failure expected")
        } catch (e: IOException) {
            assertSame(store.writeFailure, e)
            assertSame(persistenceFailure, e.suppressed.single())
        }
        assertEquals(1, store.flushed)
    }

    @Test
    fun `flush failure is propagated after successful cookie write`() {
        val failure = IOException("flush failed")
        val store = FakeCookieStore().apply { flushFailure = failure }
        val cookie = okhttp3.Cookie.Builder().name("a").value("1").domain(url.host).build()
        try {
            CookieJar(store).saveFromResponse(url, listOf(cookie))
            fail("Persistence failure expected")
        } catch (e: IOException) {
            assertSame(failure, e)
        }
        assertEquals(1, store.flushed)
    }

    @Test
    fun `session clear removes business cookies without removing other browsing sites`() = runBlocking {
        val store = FakeCookieStore()
        val otherUrl = "https://example.com/article".toHttpUrl()
        store.setCookie(url.toString(), "session=business")
        store.setCookie(otherUrl.toString(), "session=browsing")

        store.clearSession()

        assertTrue(CookieJar(store).loadForRequest(url).isEmpty())
        assertNull(store.getCookie(url.toString()))
        assertEquals("session=browsing", store.getCookie(otherUrl.toString()))
        assertEquals(1, store.flushed)
    }
}
