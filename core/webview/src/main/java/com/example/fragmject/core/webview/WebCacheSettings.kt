package com.example.fragmject.core.webview

import kotlinx.coroutines.flow.StateFlow

/**
 * WebView 缓存策略的用户可调设置。
 *
 * 与 [WebResourceCache] 分开定义的理由：缓存实现是重对象（磁盘、并发、拦截链），
 * 而设置只被「设置页」和「首次使用提示」消费；UI 不该为了读一个开关而依赖整个缓存契约。
 *
 * 当前只有一个开关：[credentialedCachingEnabled]。
 * 背景：静态资源缓存键只按 URL，因此**带身份的内容不能跨源复用**，实现上做了两级条目
 * （匿名 / 带凭据），带凭据条目仅同源命中。这个开关控制「要不要生成带凭据条目」，
 * 关掉后仍然缓存匿名内容，命中率只损失「需要登录才能拿到」的那部分。
 */
interface WebCacheSettings {

    /** 是否允许用登录态（Cookie）填充第三方资源缓存。默认开启。 */
    val credentialedCachingEnabled: StateFlow<Boolean>

    /** 写入并持久化开关；关闭时缓存实现会顺带清掉已有的带凭据条目。 */
    fun setCredentialedCachingEnabled(enabled: Boolean)

    /**
     * 「刚刚用登录态缓存过内容」的一次性提示。
     *
     * UI 展示后必须调用 [consumeCredentialedNotice] 复位，避免每次进页面都提示。
     */
    val credentialedNotice: StateFlow<Boolean>

    fun consumeCredentialedNotice()

    /** 供缓存实现标记首次使用；业务与 UI 不需要调用。 */
    fun notifyCredentialedCacheUsed()
}
