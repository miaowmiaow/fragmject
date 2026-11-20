package com.example.fragmject.core.data.contract.remote

import com.example.fragmject.core.data.contract.model.DownloadFileResult
import com.example.fragmject.core.data.contract.model.HttpResponse

/**
 * 文件下载远程数据源。
 *
 * 由 core:network 的 [com.example.fragmject.core.network.OkHttpFileDownloader]（OkHttp 流式下载）
 * 实现；core:data 只依赖本契约，不感知 OkHttp。
 */
interface DownloadRemoteDataSource {
    suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse

    /**
     * 与 [download] 相同，但额外回传缓存决策所需的响应头（见 [DownloadFileResult]）。
     *
     * 默认实现退回 [download] 并留空响应头：拿不到响应头的调用方应把内容视为**不可缓存**。
     */
    suspend fun downloadWithHeaders(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String> = emptyMap(),
    ): DownloadFileResult = DownloadFileResult(download(url, savePath, fileName, headers))
}
