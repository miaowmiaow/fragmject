package com.example.fragmject.core.webview

import android.content.Context
import android.webkit.WebView

/**
 * WebView 池生命周期契约。
 *
 * 定义在 core:webview：app 层、article/impl 与其他 WebView 消费方仅依赖本接口，
 * 不再直接依赖 WebViewPoolManager 具体实现，从而切断「组件 → 实现」耦合。
 *
 * 缓存与加载优化职责已拆至 [WebResourceCache]，与池生命周期解耦。
 * 实现见 core:webview 的 [WebViewPoolManager]（由 Hilt 以单例提供）。
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

    /**
     * 开关 WebView 远程调试（对应 WebView.setWebContentsDebuggingEnabled）。
     *
     * 收敛到本契约，避免组合根（app）直接引用 android.webkit.WebView。
     */
    fun setDebuggingEnabled(enabled: Boolean)
}
