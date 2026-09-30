package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.result.SimpleResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件下载 data 实现，委托 [DownloadRemoteDataSource]。
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloader: DownloadRemoteDataSource,
) : DownloadRepository {

    override suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): SimpleResult {
        val response = downloader.download(url, savePath, fileName, headers)
        return SimpleResult(
            success = response.errorCode == "0",
            message = response.errorMsg,
        )
    }
}
