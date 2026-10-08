package com.example.fragmject.core.android.platform.cache

/**
 * 缓存目录名集中定义。
 *
 * 目录名是一条**跨模块的隐式契约**：
 * - `coil` 由 `:app` 组合根持有（Coil DiskCache）；
 * - `okhttp` 由 `core:network` 持有（OkHttp 磁盘缓存）；
 * - `exoplayer_cache` 由 `core:player` 持有（media3 SimpleCache）；
 * - `web_cache` / `web_html_cache` 由 `core:webview` 持有。
 *
 * 而 `core:data-repository` 的 `SystemStorage` 在清理缓存时**必须排除**
 * [PROTECTED] 中的目录——它们由进程内单例长期持有，整体删除会破坏索引与锁
 * （media3 还要求同一目录只允许单一实例）。
 *
 * 分散硬编码时，任一侧改名都不会编译报错，但清理会删掉正在使用的缓存；
 * 集中定义后，目录名变更成为编译期事件。
 */
object CacheDirs {

    /** 图片加载（Coil）磁盘缓存。 */
    const val COIL = "coil"

    /** OkHttp 磁盘缓存。 */
    const val OKHTTP = "okhttp"

    /** ExoPlayer SimpleCache。 */
    const val EXOPLAYER = "exoplayer_cache"

    /** WebView 二进制资源缓存。 */
    const val WEB = "web_cache"

    /** WebView 主文档 HTML 缓存。 */
    const val WEB_HTML = "web_html_cache"

    /**
     * 由进程内单例长期持有、清理时必须排除的目录。
     *
     * 注：`WEB` / `WEB_HTML` 不在其中——WebView 缓存可安全清理，
     * 清理后最多重新下载，不存在索引/锁风险。
     */
    val PROTECTED: Set<String> = setOf(COIL, OKHTTP, EXOPLAYER)
}
