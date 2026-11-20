package com.example.fragmject.core.domain.session

/**
 * 会话残留缓存清理端口。
 *
 * 与 [CookieStore] 分离而不是合并成一个「会话清理」端口：
 * - [CookieStore] 管的是**凭证**（Cookie 的读写与撤销）；
 * - 本端口管的是**内容残留**——本地磁盘缓存、池化页面实例等可能带回上一账号内容的东西。
 *
 * 两者的清理时机相同（登出、换号、认证失败回滚），但实现归属完全不同：
 * Cookie 走 `android.webkit.CookieManager`，缓存走 `core:webview` 的磁盘缓存与 WebView 池。
 * 合在一起会让 domain 端口同时承担凭证与内容两类语义。
 *
 * 为什么必须在「清 Cookie 之后」调用：
 * 缓存清理会取消在途下载并删除目录，但清理窗口内若仍持有 Cookie，仍可能发起新的
 * 带身份请求并把响应重新写回磁盘。因此 [AuthSessionCoordinator] 的清理顺序是
 * 先撤 Cookie、再清缓存。
 *
 * 实现归属：唯一实现是 `core:webview` 的 `WebSessionCacheCleaner`
 * （经 `core:webview/di/WebViewModule` 以 `@Binds` 绑定）。
 * 清理失败不得抛出取消信号以外的异常类型差异——实现应尽力清理并把失败原样抛出，
 * 由编排方决定优先级。
 */
interface SessionCacheCleaner {

    /** 清除与登录态相关的本地缓存；协程取消原样抛出。 */
    suspend fun clearSessionCaches()

    companion object {
        /**
         * 空实现：仅用于测试替身，或宿主确实没有本地缓存时。
         *
         * 生产图必须绑定真实实现（`WebSessionCacheCleaner`），否则登出后磁盘缓存不会被清。
         */
        val NoOp: SessionCacheCleaner = object : SessionCacheCleaner {
            override suspend fun clearSessionCaches() = Unit
        }
    }
}
