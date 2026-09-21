package com.example.fragmject.core.data.contract.remote

import com.example.fragmject.core.data.contract.model.HttpResponse

/**
 * 文件下载远程数据源。
 *
 * 由 core:network 的 [com.example.fragmject.core.network.Downloader]（OkHttp 流式下载）
 * 实现；core:data 只依赖本契约，不感知 OkHttp。
 */
interface DownloadRemoteDataSource {
    suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse
}
