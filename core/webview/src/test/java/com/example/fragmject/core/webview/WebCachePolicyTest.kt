package com.example.fragmject.core.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 缓存可缓存性判定的行为基线。
 *
 * 这里的每条断言都对应一个「登录态内容会不会落盘 / 会不会被另一个会话读到」的决策点，
 * 因此改动判定时先改这里，再改实现。
 */
class WebCachePolicyTest {

    private val imageAccept = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
    private val htmlAccept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"

    // ---------- HTML 主文档 ----------

    @Test
    fun `main frame html get is cacheable`() {
        assertTrue(
            WebCachePolicy.isCacheableHtml(
                method = "GET",
                isForMainFrame = true,
                url = "https://juejin.cn/post/1",
                accept = htmlAccept,
            )
        )
    }

    @Test
    fun `no host level exception - login domain html is cacheable too`() {
        // 2026-11-20 起取消「会话域整体不缓存」：身份隔离改由「键含 Cookie + 资源条目分级 + 同源闸」承担。
        // 这条断言锁住「不再按主机名排除」这一事实，避免有人把旧限制原样加回来。
        listOf("wanandroid.com", "www.wanandroid.com", "m.wanandroid.com").forEach { host ->
            assertTrue(
                host,
                WebCachePolicy.isCacheableHtml(
                    method = "GET",
                    isForMainFrame = true,
                    url = "https://$host/article/list",
                    accept = htmlAccept,
                )
            )
        }
    }

    @Test
    fun `html cache rejects non get subframe non http and non html accept`() {
        fun cacheable(
            method: String = "GET",
            isForMainFrame: Boolean = true,
            url: String = "https://example.com/a",
            accept: String? = htmlAccept,
        ) = WebCachePolicy.isCacheableHtml(method, isForMainFrame, url, accept)

        assertFalse("POST 不能复用", cacheable(method = "POST"))
        assertFalse("子框架不是主文档", cacheable(isForMainFrame = false))
        assertFalse("非 http(s)", cacheable(url = "ftp://example.com/a"))
        assertFalse("Accept 不含 text/html", cacheable(accept = "*/*"))
        assertFalse("缺少 Accept", cacheable(accept = null))
    }

    // ---------- 静态资源 ----------

    @Test
    fun `static assets are cacheable regardless of host`() {
        listOf("jpg", "png", "webp", "css", "js", "ttf", "woff2").forEach { extension ->
            assertTrue(extension, WebCachePolicy.isCacheableResource("GET", extension, "*/*"))
        }
    }

    @Test
    fun `json is not a cacheable resource`() {
        // 回归守卫：json 是最典型的接口响应载体，历史白名单里含 json，等于把接口响应按 URL 落盘
        assertFalse(WebCachePolicy.isCacheableResource("GET", "json", "*/*"))
    }

    @Test
    fun `resource cache only accepts get`() {
        assertFalse("POST 不可缓存", WebCachePolicy.isCacheableResource("POST", "jpg", "*/*"))
        assertFalse(WebCachePolicy.isCacheableResource("PUT", "css", "*/*"))
    }

    @Test
    fun `extensionless requests are cacheable only with the exact image accept`() {
        assertTrue(WebCachePolicy.isCacheableResource("GET", "", imageAccept))
        assertFalse("XHR 的通配 Accept 不能当图片", WebCachePolicy.isCacheableResource("GET", "", "*/*"))
    }

    // ---------- 源站缓存指令 ----------

    @Test
    fun `no-store and private responses are never stored`() {
        assertFalse(WebCachePolicy.allowsStorage("no-store", null))
        assertFalse(WebCachePolicy.allowsStorage("private", null))
        assertFalse(WebCachePolicy.allowsStorage("max-age=0, private, must-revalidate", null))
        assertFalse(WebCachePolicy.allowsStorage("No-Store, max-age=60", null))
    }

    @Test
    fun `no-cache is storable because html uses revalidate in background`() {
        assertTrue(WebCachePolicy.allowsStorage("no-cache", null))
        assertTrue(WebCachePolicy.allowsStorage(null, null))
        assertTrue(WebCachePolicy.allowsStorage("public, max-age=600", null))
    }

    @Test
    fun `merged multi-line directives are still rejected`() {
        // 下载器会把同名字段按 HTTP 语义以逗号合并（见 core:network 的 cacheRelevantHeaders），
        // 这里锁住「合并后的字符串仍能被正确拒绝」，避免修好提取又漏在解析。
        assertFalse(WebCachePolicy.allowsStorage("max-age=3600, no-store", null))
        assertFalse(WebCachePolicy.allowsStorage("public, max-age=600, private", null))
        assertFalse(WebCachePolicy.allowsStorage(null, "Accept-Encoding, Cookie"))
    }

    @Test
    fun `only accept-encoding vary dimensions are reproducible`() {
        assertTrue(WebCachePolicy.allowsStorage(null, "Accept-Encoding"))
        assertFalse("key 无法复现语言维度", WebCachePolicy.allowsStorage(null, "Accept-Language"))
        assertFalse("Vary 星号无法复现", WebCachePolicy.allowsStorage(null, "*"))
        assertFalse(
            "混入不可复现维度即不可缓存",
            WebCachePolicy.allowsStorage(null, "Accept-Encoding, Accept-Language"),
        )
    }

    // ---------- 响应类型闸门 ----------

    @Test
    fun `html storage requires a text html content type`() {
        assertTrue(WebCachePolicy.isStorableHtml("text/html; charset=utf-8"))
        assertTrue(WebCachePolicy.isStorableHtml("TEXT/HTML"))
        assertFalse("拿不到 Content-Type 时不缓存", WebCachePolicy.isStorableHtml(null))
        assertFalse(WebCachePolicy.isStorableHtml(""))
        assertFalse("接口响应不能当文档缓存", WebCachePolicy.isStorableHtml("application/json"))
    }

    @Test
    fun `resource storage accepts static types and rejects documents and apis`() {
        assertTrue(WebCachePolicy.isStorableResource("image/jpeg"))
        assertTrue(WebCachePolicy.isStorableResource("text/css; charset=utf-8"))
        assertTrue(WebCachePolicy.isStorableResource("font/woff2"))
        assertTrue(WebCachePolicy.isStorableResource("application/javascript"))
        assertFalse(WebCachePolicy.isStorableResource("text/html; charset=utf-8"))
        assertFalse(WebCachePolicy.isStorableResource("application/json; charset=utf-8"))
        assertFalse(WebCachePolicy.isStorableResource(null))
    }

    @Test
    fun `cors headers echo a valid origin exactly`() {
        val expected = mapOf("Access-Control-Allow-Origin" to "https://juejin.cn")
        assertEquals(expected, WebCachePolicy.corsHeaders("https://juejin.cn"))
        assertEquals(expected, WebCachePolicy.corsHeaders("  https://juejin.cn  "))
        assertEquals(
            mapOf("Access-Control-Allow-Origin" to "http://a.b:8080"),
            WebCachePolicy.corsHeaders("http://a.b:8080"),
        )
    }

    @Test
    fun `unrecognized origins fall back to wildcard instead of dropping the header`() {
        // fail-open：识别不出来源时降级为 `*`，而不是不给头。
        // 不给头会让「Origin 头在拦截层不可见」的 WebView 上命中缓存后 CORS 失败（图片转圈）。
        val unrecognized = listOf(
            null, "", "   ", "null", "NULL", "*",
            "https://*.a.com", "https://a.com/x", "capability://x", "http://",
        )
        unrecognized.forEach { origin ->
            assertEquals(
                "origin=$origin",
                mapOf("Access-Control-Allow-Origin" to "*"),
                WebCachePolicy.corsHeaders(origin),
            )
        }
    }

    @Test
    fun `cors headers never grant credentialed reads`() {
        // 不变量：只有 ACAO 一个键，且永不出现 Allow-Credentials。
        // 有了 Allow-Credentials，回显 Origin 就等于允许带凭据跨域读。
        val origins = listOf(
            null, "", "   ", "null", "NULL", "*",
            "https://juejin.cn", "http://a.b:8080", "https://www.wanandroid.com",
            "https://*.a.com", "capability://x",
        )
        origins.forEach { origin ->
            val headers = WebCachePolicy.corsHeaders(origin)
            assertTrue(
                "必须始终给出 ACAO（fail-open）：origin=$origin",
                headers.containsKey("Access-Control-Allow-Origin"),
            )
            assertFalse(
                "不得输出 Allow-Credentials：origin=$origin headers=$headers",
                headers.keys.any { it.equals("Access-Control-Allow-Credentials", true) },
            )
            assertEquals(
                "只允许出现 ACAO 这一个键：origin=$origin headers=$headers",
                setOf("Access-Control-Allow-Origin"),
                headers.keys,
            )
        }
    }

    // ---------- Content-Type 解析 ----------

    @Test
    fun `content type parsing keeps charset and normalizes mime`() {
        // mime 归一化为小写；charset 原样透传（WebResourceResponse 的 encoding 本身大小写不敏感）
        assertEquals(
            ParsedContentType("text/html", "UTF-8"),
            WebCachePolicy.parseContentType("text/html; charset=UTF-8"),
        )
        assertEquals(
            ParsedContentType("text/css", "gbk"),
            WebCachePolicy.parseContentType("text/css;charset=\"gbk\""),
        )
        assertEquals(ParsedContentType("text/html", null), WebCachePolicy.parseContentType("text/html"))
        assertEquals(ParsedContentType("", null), WebCachePolicy.parseContentType(null))
        assertEquals(ParsedContentType("", null), WebCachePolicy.parseContentType("   "))
    }

    @Test
    fun `response header lookup is case insensitive`() {
        val headers = mapOf("cache-control" to "no-store", "Content-Type" to "text/html")
        assertEquals("no-store", headers.headerValue("Cache-Control"))
        assertNull(headers.headerValue("Vary"))
    }

    // ---------- 缓存键 ----------

    @Test
    fun `html cache key separates accounts by cookie`() {
        val url = "https://example.com/a"
        assertEquals(
            WebCachePolicy.htmlCacheKey(url, null),
            WebCachePolicy.htmlCacheKey(url, ""),
        )
        assertNotEquals(
            WebCachePolicy.htmlCacheKey(url, "session=a"),
            WebCachePolicy.htmlCacheKey(url, "session=b"),
        )
    }

    @Test
    fun `resource entry names are url based and split by credential level`() {
        val url = "https://cdn.example.com/a.jpg"
        val anonymous = WebCachePolicy.resourceEntryName(url, credentialed = false)
        val credentialed = WebCachePolicy.resourceEntryName(url, credentialed = true)

        assertEquals(anonymous, WebCachePolicy.resourceEntryName(url, credentialed = false))
        assertNotEquals(anonymous, credentialed)
        assertNotEquals(
            anonymous,
            WebCachePolicy.resourceEntryName("https://cdn.example.com/b.jpg", credentialed = false),
        )
        assertTrue(WebCachePolicy.isCredentialedEntry(credentialed))
        assertFalse(WebCachePolicy.isCredentialedEntry(anonymous))
        assertTrue(WebCachePolicy.isCredentialedEntry("$credentialed.part"))
    }

    @Test
    fun `credentialed entries only participate in same host lookups`() {
        val url = "https://cdn.example.com/a.jpg"
        val anonymous = WebCachePolicy.resourceEntryName(url, credentialed = false)
        val credentialed = WebCachePolicy.resourceEntryName(url, credentialed = true)

        // 同源：优先带凭据条目，再用匿名条目兜底
        assertEquals(listOf(credentialed, anonymous), WebCachePolicy.resourceEntryNames(url, sameHost = true))
        // 跨源：只允许匿名条目（带身份的内容不得交给别的站点读取）
        assertEquals(listOf(anonymous), WebCachePolicy.resourceEntryNames(url, sameHost = false))
    }

    // ---------- 缓存填充请求头 ----------

    @Test
    fun `cache fill headers only forward identity free fields`() {
        val source = mapOf(
            "user-agent" to "UA/1.0",
            "Accept" to "image/webp",
            "Accept-Language" to "zh-CN",
            "Authorization" to "Bearer secret",
            "Cookie" to "session=stolen",
            "X-Auth-Token" to "secret",
        )

        val headers = WebCachePolicy.cacheFillHeaders(source, referer = null, cookie = null)

        // 白名单转发的三个内容协商字段（名字统一为标准大小写）
        assertEquals("UA/1.0", headers["User-Agent"])
        assertEquals("image/webp", headers["Accept"])
        assertEquals("zh-CN", headers["Accept-Language"])
        // 身份字段一律不转发：带身份的内容会被写进磁盘缓存
        assertFalse(headers.containsKey("Authorization"))
        assertFalse(headers.containsKey("Cookie"))
        assertFalse(headers.containsKey("X-Auth-Token"))
        assertNull(headers["Referer"])
    }

    @Test
    fun `cache fill headers carry credentials only when explicitly requested`() {
        val source = mapOf("User-Agent" to "UA/1.0")

        val anonymous = WebCachePolicy.cacheFillHeaders(source, referer = "https://a.com/p", cookie = null)
        assertEquals("https://a.com/p", anonymous["Referer"])
        assertFalse(anonymous.containsKey("Cookie"))

        val credentialed = WebCachePolicy.cacheFillHeaders(source, referer = null, cookie = "session=1")
        assertEquals("session=1", credentialed["Cookie"])
    }

    // ---------- 同源判定 ----------

    @Test
    fun `same host compares host only`() {
        assertTrue(WebCachePolicy.isSameHost("https://a.com/p/1", "https://a.com/img/x.png"))
        assertTrue(WebCachePolicy.isSameHost("http://A.COM/p", "https://a.com/x.png"))
        assertFalse("子域不是同主机", WebCachePolicy.isSameHost("https://m.a.com/p", "https://a.com/x.png"))
        assertFalse("不同站点", WebCachePolicy.isSameHost("https://evil.com/p", "https://a.com/x.png"))
        assertFalse("拿不到来源时按跨源处理", WebCachePolicy.isSameHost(null, "https://a.com/x.png"))
        assertFalse(WebCachePolicy.isSameHost("https://a.com/p", null))
    }
}
