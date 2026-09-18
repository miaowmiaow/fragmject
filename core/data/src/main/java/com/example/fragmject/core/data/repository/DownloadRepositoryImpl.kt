package com.example.fragmject.core.data.repository

import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.result.DownloadResult
import com.example.fragmject.core.network.Downloader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件下载 data 实现，委托 [Downloader]。
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloader: Downloader,
) : DownloadRepository {

    override suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): DownloadResult {
        val response = downloader.download(url, savePath, fileName, headers)
        return DownloadResult(
            success = response.errorCode == "0",
            message = response.errorMsg,
        )
    }
}
