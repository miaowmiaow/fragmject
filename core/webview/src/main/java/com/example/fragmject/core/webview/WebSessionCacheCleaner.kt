package com.example.fragmject.core.webview

import com.example.fragmject.core.domain.session.SessionCacheCleaner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [SessionCacheCleaner] 的 WebView 实现。
 *
 * 登出 / 换号时清除两类残留：
 * 1. **磁盘缓存**（[WebResourceCache.clearAll]）：`web_cache` / `web_html_cache`，
 *    同时取消在途下载，避免删除后又被写回；
 * 2. **池化 WebView 实例**（[WebViewPool.releaseAll]）：keep-alive 池里的实例保留着
 *    上一会话已渲染的 DOM 与 JS 上下文，同 URL 再次打开会被直接复用（不重新加载），
 *    等于把上一账号的页面显示给下一个人。
 *
 * 顺序固定：先清缓存，再释放池。反过来的话，仍存活的实例可能立刻重新发起加载，
 * 把带身份的响应又写回刚清空的目录。
 *
 * WebView 必须在创建它的主线程销毁（`destroy()` 亦然），因此释放池切到 Main。
 */
@Singleton
class WebSessionCacheCleaner @Inject constructor(
    private val cache: WebResourceCache,
    private val pool: WebViewPool,
) : SessionCacheCleaner {

    override suspend fun clearSessionCaches() {
        cache.clearAll()
        withContext(Dispatchers.Main.immediate) { pool.releaseAll() }
    }
}
