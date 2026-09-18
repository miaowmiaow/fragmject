package com.example.fragmject.feature.article

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

/**
 * WebView 池生命周期与缓存契约。
 *
 * 定义在 article/api：app 层与 article/impl 内部的 WebView 组件仅依赖本接口，
 * 不再直接依赖 impl 内部的 WebViewManager 实现，从而切断「组件 → impl」耦合。
 *
 * 实现见 article/impl 的 [WebViewManager]（由 Hilt 以单例提供）。
 */
interface WebViewPool {
    /** 应用启动后预创建一个空闲 WebView，供下次 obtain 复用。 */
    fun prepare(context: Context)

    /** 获取一个 WebView 实例（keep-alive 命中 → 空闲位 → 全新创建）。 */
    fun obtain(context: Context, url: String): WebView

    /** 回收 WebView：解绑回调、切回 ApplicationContext，视情况进入 keep-alive 池或销毁。 */
    fun recycle(webView: WebView)

    /** 内存吃紧时清空 keep-alive 池，仅保留一个空闲热身实例。 */
    fun trimToSpare()

    /** 极端缺内存时彻底释放所有 WebView。 */
    fun releaseAll()

    /** 判断请求是否为可缓存资源（图片/样式/脚本/字体等）。 */
    fun isCacheResource(request: WebResourceRequest): Boolean

    /** 命中本地磁盘缓存则返回响应；未命中返回 null 并异步下载填充缓存。 */
    fun cacheResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse?

    /** 预解析 URL 的主机名，降低首次连接延迟。 */
    fun prefetchDns(url: String)
}
