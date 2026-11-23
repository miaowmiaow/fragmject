package com.example.fragmject.core.webview

import android.util.Log
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.DownloadResource
import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.android.platform.log.LogSanitizer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 静态资源缓存填充：决定「这一份要不要带登录态」，并负责落盘。
 *
 * ## 为什么需要「探测」
 *
 * 资源缓存键只按 URL（见 [WebCachePolicy.resourceEntryNames]），URL 键能成立的前提是
 * **落盘内容与身份无关**。但有些资源（头像、需要登录才可见的图）必须带 Cookie 才拿得到。
 * 因此按主机做**一次性探测**：
 *
 * 1. 先匿名下载——成功即落盘为「匿名条目」，内容在构造上与身份无关，任何来源可命中；
 * 2. 匿名失败（401/403/网络错误）且该 URL 有 Cookie、开关允许时，带 Cookie 重试一次；
 * 3. 结论按主机记在内存里（[hostDecisions]），同主机后续资源不再重复探测，
 *    避免「每个资源两次请求」的流量与时延代价。
 *
 * ## 两级条目
 *
 * - 匿名条目：任何来源可命中；
 * - 带凭据条目（`.cred` 后缀）：**只允许同源命中**，由 [WebResourceCacheManager] 在命中路径上把关。
 *
 * 开关关闭时不会生成带凭据条目，并会清掉已有的（[purgeCredentialed]）。
 */
@Singleton
class WebResourceFetcher @Inject constructor(
    private val downloader: DownloadRepository,
    private val cookieStore: CookieStore,
    private val settings: WebCacheSettings,
) {

    /** 主机 → 是否需要登录态才能取到资源；true 表示「带凭据」。内存态，登出/清缓存时重置。 */
    private val hostDecisions = ConcurrentHashMap<String, Boolean>()

    /**
     * 下载并落盘一个静态资源，成功返回 true。
     *
     * [referer] 为发起请求的页面 URL（防盗链需要）；[requestHeaders] 为 WebView 原始请求头，
     * 只会按 [WebCachePolicy.cacheFillHeaders] 的白名单转发。
     */
    suspend fun fetch(
        cachePath: String,
        url: String,
        referer: String?,
        requestHeaders: Map<String, String>,
        host: String,
    ): Boolean {
        val hostKey = host.lowercase()
        if (hostKey.isEmpty()) return false
        val cookie = cookieStore.getCookie(url)?.takeIf { it.isNotBlank() }
        val known = hostDecisions[hostKey]

        // 已判定该主机需要登录态，且 Cookie 仍在：直接带凭据
        if (known == true && cookie != null) {
            return download(cachePath, url, referer, requestHeaders, cookie = cookie, credentialed = true)
        }

        // 匿名尝试：首次探测、已判定匿名、或凭据已失效（降级为匿名，不影响展示）
        if (download(cachePath, url, referer, requestHeaders, cookie = null, credentialed = false)) {
            if (known == null) hostDecisions[hostKey] = false
            return true
        }
        // 已有结论时不再二次探测，避免每个资源两次请求
        if (known != null) return false
        if (!settings.credentialedCachingEnabled.value || cookie == null) {
            // 无凭据可用或用户关闭了开关：该主机按匿名处理
            hostDecisions[hostKey] = false
            return false
        }

        // 首次探测：匿名失败 → 带凭据重试一次，并把结论记住
        val credentialedOk = download(cachePath, url, referer, requestHeaders, cookie = cookie, credentialed = true)
        hostDecisions[hostKey] = credentialedOk
        if (credentialedOk) settings.notifyCredentialedCacheUsed()
        return credentialedOk
    }

    /** 删除所有带凭据条目（用户关闭开关时调用；匿名条目保留）。 */
    fun purgeCredentialed(cachePath: String) {
        try {
            File(cachePath).listFiles()
                ?.filter { WebCachePolicy.isCredentialedEntry(it.name) }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w(TAG, "purgeCredentialed failed", e)
        }
    }

    /** 会话变更时清空主机结论：换号后要重新探测（原结论属于上一个会话）。 */
    fun reset() {
        hostDecisions.clear()
    }

    /**
     * 单次下载 + 校验 + 原子 rename。
     *
     * 落盘闸门与 HTML 一致：源站禁止缓存（`no-store` / `private` / 无法复现的 `Vary`）
     * 或响应不是静态资源类型时直接丢弃 `.part`，不写目标文件。
     */
    private suspend fun download(
        cachePath: String,
        url: String,
        referer: String?,
        requestHeaders: Map<String, String>,
        cookie: String?,
        credentialed: Boolean,
    ): Boolean {
        val entry = WebCachePolicy.resourceEntryName(url, credentialed)
        val partName = entry + PART_SUFFIX
        val partFile = File(cachePath, partName)
        return try {
            val resource = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS.milliseconds) {
                downloader.downloadResource(
                    url = url,
                    savePath = cachePath,
                    fileName = partName,
                    headers = WebCachePolicy.cacheFillHeaders(requestHeaders, referer, cookie),
                )
            }
            val downloaded = resource.takeIfDownloaded(partFile)
            if (downloaded == null || !isStorable(downloaded.responseHeaders)) {
                partFile.delete()
                return false
            }
            val target = File(cachePath, entry)
            if (target.exists()) target.delete()
            if (!partFile.renameTo(target)) {
                partFile.delete()
                return false
            }
            target.setLastModified(System.currentTimeMillis())
            true
        } catch (e: CancellationException) {
            partFile.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "download failed: ${LogSanitizer.url(url)}", e)
            partFile.delete()
            false
        }
    }

    /** 下载产出了可用的完整文件时返回结果，否则返回 null。 */
    private fun DownloadResource?.takeIfDownloaded(file: File): DownloadResource? =
        this?.takeIf { it.result.success && file.isFile && file.length() > 0L }

    /** 静态资源落盘闸门：源站策略 + 响应类型。 */
    private fun isStorable(headers: Map<String, String>): Boolean =
        WebCachePolicy.allowsStorage(
            cacheControl = headers.headerValue("Cache-Control"),
            vary = headers.headerValue("Vary"),
        ) && WebCachePolicy.isStorableResource(headers.headerValue("Content-Type"))

    private companion object {
        const val TAG = "WebResourceFetcher"
        const val DOWNLOAD_TIMEOUT_MS = 8_000L
        const val PART_SUFFIX = ".part"
    }
}
