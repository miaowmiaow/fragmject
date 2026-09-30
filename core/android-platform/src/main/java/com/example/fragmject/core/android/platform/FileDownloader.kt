package com.example.fragmject.core.android.platform

/**
 * 轻量文件下载能力抽象。
 *
 * 定义在 android-platform，作为「把远端资源下载到本地磁盘」的平台级能力端口，
 * 供 WebView 资源缓存等基础设施复用，避免基础设施层（core:webview）反向依赖
 * 业务领域层（core:domain 的 DownloadRepository）。
 *
 * 实现由 data 层在 DI 时把 core:domain 的 DownloadRepository 桥接进来。
 */
fun interface FileDownloader {
    /**
     * 下载远端资源到本地文件。
     *
     * @return 是否下载成功（文件已完整写入 [savePath]/[fileName]）。
     */
    suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): Boolean
}
