package com.example.fragmject.core.data.repository

import android.util.Log
import com.example.fragmject.core.data.contract.model.DownloadFileResult
import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import com.example.fragmject.core.data.repository.util.fetchAsSimpleResult
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.DownloadResource
import com.example.fragmject.core.domain.result.SimpleResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 文件下载 data 实现，委托 [DownloadRemoteDataSource]。
 *
 * [download] 由 [downloadResource] 派生，保证「是否成功」只有一套判定口径；
 * 响应头只在成功时透出，失败时为空（缓存侧据此不落盘）。
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
    ): SimpleResult = downloadResource(url, savePath, fileName, headers).result

    override suspend fun downloadResource(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): DownloadResource {
        val fileResult: DownloadFileResult = try {
            downloader.downloadWithHeaders(url, savePath, fileName, headers)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "download failed", e)
            return DownloadResource(SimpleResult(success = false, message = FAILURE_MESSAGE))
        }
        val result = fetchAsSimpleResult(failureMessage = FAILURE_MESSAGE) { fileResult.response }
        return DownloadResource(
            result = result,
            responseHeaders = if (result.success) fileResult.headers else emptyMap(),
        )
    }

    private companion object {
        const val TAG = "DownloadRepository"
        const val FAILURE_MESSAGE = "下载失败"
    }
}
