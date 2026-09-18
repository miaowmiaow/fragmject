package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DownloadResult

/**
 * 文件下载领域端口。
 *
 * 抽象 HTTP 文件下载能力，供 feature 层（WebView 资源缓存、m3u8 下载等）
 * 通过领域接口使用，由 data 层基于网络基础设施实现。
 */
interface DownloadRepository {
    suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String> = emptyMap(),
    ): DownloadResult
}
