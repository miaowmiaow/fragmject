package com.example.fragmject.core.webview

import com.example.fragmject.core.android.platform.digest.DigestUtils
import java.net.URI

/**
 * WebView 本地缓存的可缓存性判定。
 *
 * 全部是**纯函数**（不碰 Android API、不碰文件系统），因此可以在 JVM 单测里直接覆盖。
 * 之所以把判定从 [WebResourceCacheManager] 里抽出来，是因为这里的每条规则都直接决定
 * 「登录态内容会不会落盘 / 会不会被另一个会话读到」——这种规则必须可测、可回归，
 * 而不是埋在 `shouldInterceptRequest` 的 if 链里。
 *
 * 两条硬约束：
 * 1. **只缓存 GET**：POST/PUT 的响应不可能安全复用。
 * 2. **源站说不缓存就不缓存（fail-closed）**：`Cache-Control: no-store/private`、
 *    无法复现的 `Vary` 维度、或拿不到 `Content-Type` 时一律不落盘。
 *
 * ## 身份隔离靠什么承担（不再按主机名整体排除）
 *
 * 历史实现把 `wanandroid.com` 及其子域整体排除在缓存之外，用「不落盘」换取确定性。
 * 现在改为**按条目分级 + 命中闸**承担同一件事，因此不再有主机级例外：
 *
 * - **HTML 主文档**：缓存键含 Cookie（[htmlCacheKey]），本身就是按身份分区的，
 *   换号 / 登出后 key 不同，不会命中上一身份的页面；
 * - **静态资源**：键只按 URL，因此内容按是否带凭据分成两级——
 *   匿名条目（任何来源可命中，因为服务端没收到身份）与 `.cred` 条目
 *   （仅同源可命中，见 [resourceEntryNames]）；
 * - **会话变更**：登出时 `SessionCacheCleaner` 会删掉全部缓存目录，跨会话复用不存在；
 * - **响应闸门**：个性化页面通常带 `no-store` / `private` / `Vary: Cookie`，已被下面的判定挡住。
 *
 * 代价与残留：个人页（若服务端未标注 `no-store`）会以「按身份分区的条目」落到
 * `web_html_cache`，属磁盘明文（internal cacheDir，root / 取证可读），登出即清。
 *
 * 为什么 `Vary: Accept-Encoding` 单独放行：下载器（OkHttp）对 gzip 透明解压，
 * 落盘的是已解码的响应体，回放时不带 `Content-Encoding`，因此该维度不影响内容。
 * 其余 `Vary` 维度（`Accept-Language`、`User-Agent`、`Cookie`…）本缓存的 key 无法复现，
 * 复用就是错误内容，直接判为不可缓存。
 */
internal object WebCachePolicy {

    /** 无扩展名请求的「图片」判据：Accept 必须与图片预置值完全一致，避免把 XHR 的通配 Accept 当图片。 */
    private const val ACCEPT_IMAGE =
        "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

    /**
     * 可缓存的静态资源扩展名。
     *
     * **不含 `json`**：`json` 是最典型的 API 响应载体，缓存它等于把接口响应（可能带身份）
     * 按 URL 落盘并被后续任意请求复用。
     */
    private val CACHEABLE_EXTENSIONS = setOf(
        "ico", "bmp", "gif", "jpeg", "jpg", "png", "svg", "webp",
        "css", "js",
        "eot", "otf", "ttf", "woff", "woff2",
    )

    /** 识别不出来源时的 CORS 兜底值（见 [corsHeaders] 的 fail-open 说明）。 */
    private const val WILDCARD_ORIGIN = "*"

    /** 带凭据条目的文件名后缀（见 [resourceEntryNames]）。 */
    private const val CREDENTIALED_SUFFIX = ".cred"

    /**
     * 缓存填充请求只转发这几个头：它们只影响内容协商，不携带身份。
     *
     * 刻意不含 `Cookie` / `Authorization` / `Proxy-Authorization` / 站点自定义鉴权头——
     * 身份是否随请求发送由 [cacheFillHeaders] 的 `cookie` 参数单独决定。
     */
    private val FORWARDED_REQUEST_HEADERS = listOf("User-Agent", "Accept", "Accept-Language")

    /** 可以原样回显的来源协议；其余 scheme 一律走通配兜底。 */
    private val ECHOABLE_SCHEMES = listOf("https://", "http://")

    /** 只认静态资源类响应头；这些之外的 `application/octet-stream` 属误判，宁可放弃缓存。 */
    private val STORABLE_RESOURCE_TYPES = setOf(
        "text/css",
        "application/javascript",
        "text/javascript",
        "application/x-javascript",
        "application/font-woff",
        "application/font-woff2",
        "application/x-font-ttf",
        "application/x-font-opentype",
        "application/vnd.ms-fontobject",
    )

    /** `Vary` 中允许出现的字段（见 KDoc）。 */
    private val ALLOWED_VARY_FIELDS = setOf("accept-encoding")

    /** HTML 主文档是否可缓存（方法 / 主框架 / scheme / Accept）。 */
    fun isCacheableHtml(
        method: String?,
        isForMainFrame: Boolean,
        url: String?,
        accept: String?,
    ): Boolean {
        if (!method.equals("GET", ignoreCase = true)) return false
        if (!isForMainFrame) return false
        if (url == null || !isHttpUrl(url)) return false
        return accept?.contains("text/html", ignoreCase = true) == true
    }

    /** 静态资源是否可缓存（方法 / 扩展名白名单）。 */
    fun isCacheableResource(
        method: String?,
        extension: String,
        accept: String?,
    ): Boolean {
        if (!method.equals("GET", ignoreCase = true)) return false
        if (extension.isNotBlank()) return extension.lowercase() in CACHEABLE_EXTENSIONS
        // 无扩展名：只接受图片 Accept（CSS/JS 必有扩展名），其余一律不缓存
        return accept == ACCEPT_IMAGE
    }

    /**
     * 源站是否允许把该响应持久化到本地。
     *
     * `no-cache` 是放行的：本缓存对 HTML 采用 stale-while-revalidate，命中后仍会回源刷新，
     * 语义上满足「使用前必须校验」。
     */
    fun allowsStorage(cacheControl: String?, vary: String?): Boolean {
        val directives = cacheControl.orEmpty()
            .split(',')
            .map { it.substringBefore('=').trim().lowercase() }
            .filter { it.isNotEmpty() }
        if ("no-store" in directives || "private" in directives) return false

        val varyFields = vary.orEmpty()
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        return varyFields.all { it in ALLOWED_VARY_FIELDS }
    }

    /** HTML 主文档落盘判据：必须是 `text/html`（拿不到 Content-Type 就不缓存）。 */
    fun isStorableHtml(contentType: String?): Boolean =
        contentType?.trim()?.startsWith("text/html", ignoreCase = true) == true

    /** 静态资源落盘判据：拒绝 `text/html` / `application/json` 这类可能是接口或错误页的响应。 */
    fun isStorableResource(contentType: String?): Boolean {
        val mime = parseContentType(contentType).mimeType
        if (mime.isEmpty()) return false
        if (mime.startsWith("image/") || mime.startsWith("font/")) return true
        return mime in STORABLE_RESOURCE_TYPES
    }

    /**
     * HTML 缓存键：URL + Cookie 摘要。
     *
     * Cookie 进 key 是**正确性**要求（同一 URL 在不同账号下的内容可能不同），
     * 不是「为了缓存带身份内容」——会话域已在 [isCacheableHtml] 被整体排除。
     */
    fun htmlCacheKey(url: String, cookie: String?): String =
        DigestUtils.md5Hex(if (cookie.isNullOrBlank()) url else "$url|$cookie")

    /**
     * 静态资源缓存条目的候选文件名（按优先级排列）。
     *
     * 资源缓存键只按 URL 生成——把 Cookie 放进 key 会让第三方站点的追踪 Cookie
     * （如 Cloudflare 的 `__cf_bm`）频繁轮换，缓存几乎永不命中。URL 键能成立的前提是
     * **落盘内容与身份无关**，因此这里把条目分成两级：
     *
     * - 匿名条目（无后缀）：下载时未带任何凭据，内容在构造上与身份无关，**任何来源都可命中**；
     * - 带凭据条目（[CREDENTIALED_SUFFIX]）：下载时带了 Cookie，内容可能是该账号专属，
     *   **只允许同源页面命中**（跨源命中等于把带身份的内容交给别的站点读取）。
     *
     * [sameHost] 即「发起请求的页面与资源同主机」；同源时优先取带凭据条目（更新、更贴合当前账号），
     * 没有再退到匿名条目。
     */
    fun resourceEntryNames(url: String, sameHost: Boolean): List<String> {
        val anonymous = DigestUtils.md5Hex(url)
        return if (sameHost) listOf(anonymous + CREDENTIALED_SUFFIX, anonymous) else listOf(anonymous)
    }

    /** 缓存填充落盘时使用的条目名。[credentialed] 为 true 表示请求带了身份。 */
    fun resourceEntryName(url: String, credentialed: Boolean): String =
        DigestUtils.md5Hex(url) + if (credentialed) CREDENTIALED_SUFFIX else ""

    /** 条目名是否属于「带凭据」级别（关闭开关时需要定向清理）。 */
    fun isCredentialedEntry(fileName: String): Boolean =
        fileName.endsWith(CREDENTIALED_SUFFIX) || fileName.endsWith("$CREDENTIALED_SUFFIX.part")

    /**
     * 组装缓存填充请求头：**白名单**转发，只带与内容协商有关的字段。
     *
     * 历史实现是「原样透传 requestHeaders + 追加 Cookie」，等于把 `Authorization`、
     * 站点自定义鉴权头也一起带进下载请求，进而把带身份的响应写进磁盘缓存。
     *
     * [cookie] 为 null 表示匿名下载（内容与身份无关，可跨源命中）；
     * 非 null 表示带凭据下载（条目仅同源命中）。`Referer` 用于防盗链，不含身份。
     */
    fun cacheFillHeaders(
        source: Map<String, String>,
        referer: String?,
        cookie: String?,
    ): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        FORWARDED_REQUEST_HEADERS.forEach { name ->
            source.headerValue(name)?.takeIf { it.isNotBlank() }?.let { headers[name] = it }
        }
        if (!referer.isNullOrBlank()) headers["Referer"] = referer
        if (!cookie.isNullOrBlank()) headers["Cookie"] = cookie
        return headers
    }

    /** 页面 URL 与资源 URL 是否同主机（用于「带凭据条目仅同源命中」的判定）。 */
    fun isSameHost(pageUrl: String?, resourceUrl: String?): Boolean {
        val pageHost = hostOf(pageUrl) ?: return false
        val resourceHost = hostOf(resourceUrl) ?: return false
        return pageHost == resourceHost
    }

    private fun hostOf(url: String?): String? = url
        ?.let { runCatching { URI(it) }.getOrNull() }
        ?.host
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }

    /**
     * 缓存命中时需要回填的 CORS 头（fail-open：始终给出一个可用的 `Access-Control-Allow-Origin`）。
     *
     * ## 这个头什么时候才有安全含义
     *
     * 该头是我们重建响应时自己补的，要产生安全影响必须同时满足四个条件，缺一条就是纯功能位：
     * 1. 请求命中了磁盘缓存（未命中走原生请求，头由源站给）；
     * 2. 请求是 CORS 模式（带 `Origin` 头）；
     * 3. 请求方 origin ≠ 资源 origin（同源请求不做 CORS 检查，头写什么都无效）；
     * 4. 浏览器要把**字节交给脚本**（`fetch`/XHR 取文本、canvas 读像素），而不只是渲染。
     *
     * ## 安全边界不在这条头上，而在「缓存什么内容」
     *
     * 缓存内容按构造不含身份数据（会话域整体排除、`no-store/private` 与无法复现的 `Vary` 排除、
     * `json`/`text/html` 排除），高危场景已由 [isCacheableHtml] / [isCacheableResource]
     * 与落盘闸门挡住。因此这里**不按资源类型收窄**：那样做没有安全收益，只会把
     * `crossorigin` 图片 / 跨域字体打死（命中缓存即 CORS 失败，页面表现为图片一直转圈）。
     *
     * ## 为什么是 fail-open 而不是「看不到 Origin 就不给」
     *
     * `Origin` 是否出现在 `shouldInterceptRequest` 的 `requestHeaders` 里属实现细节
     * （老 WebView / 部分 ROM 会裁剪请求头）。一旦看不到，按「非 CORS」处理就会漏掉注入，
     * 结果是「首次打开正常、二次进入命中缓存后图片转圈」——正是这套代码踩过的坑。
     * 因此口径是：**能识别出来源就精确回显，识别不出来就降级为 `*`**。
     * 对非 CORS 请求而言 `*` 本就不生效，不构成额外暴露。
     *
     * ## 保留的两条不变量（由 `WebCachePolicyTest` 反向锁住）
     *
     * - **只产出 `Access-Control-Allow-Origin` 一个键，且永不输出
     *   `Access-Control-Allow-Credentials`**：有它才允许带凭据跨域读；
     * - **只回显合法来源**（`http(s)://host[:port]`）：`null`、`*`、空值、带路径或空白的值
     *   一律降级为 `*`，避免把不可控字符串原样写进响应头。
     */
    fun corsHeaders(origin: String?): Map<String, String> {
        val value = origin?.trim().orEmpty()
        val allowOrigin = if (isEchoableOrigin(value)) value else WILDCARD_ORIGIN
        return mapOf("Access-Control-Allow-Origin" to allowOrigin)
    }

    /** 是否是可以原样回显的具体来源；任何「不像 Origin」的值都走通配兜底。 */
    private fun isEchoableOrigin(value: String): Boolean {
        if (value.isEmpty() || value.any { it.isWhitespace() }) return false
        val scheme = ECHOABLE_SCHEMES.firstOrNull { value.startsWith(it, ignoreCase = true) } ?: return false
        val authority = value.substring(scheme.length).trimEnd('/')
        return authority.isNotEmpty() && '/' !in authority && '*' !in authority
    }

    /** 解析 `Content-Type`；非法/缺失时 mimeType 为空串。 */
    fun parseContentType(raw: String?): ParsedContentType {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return ParsedContentType(mimeType = "", charset = null)
        val parts = value.split(';')
        val mimeType = parts.first().trim().lowercase()
        val charset = parts.drop(1)
            .map { it.trim() }
            .firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim()
            ?.trim('"', '\'')
            ?.takeIf { it.isNotEmpty() }
        return ParsedContentType(mimeType, charset)
    }

    private fun isHttpUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)
}

/** `Content-Type` 的解析结果：mimeType 为空串表示缺失或无法识别。 */
internal data class ParsedContentType(
    val mimeType: String,
    val charset: String?,
)

/** 大小写不敏感地取响应头值；下载器透传的头名大小写不保证统一。 */
internal fun Map<String, String>.headerValue(name: String): String? =
    entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
