package com.example.fragmject.core.network

import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 流式文件下载器。
 *
 * 替代原 CoroutineHttp.download：不走 Retrofit，直接用 OkHttp 流式写文件。
 * 使用下载专用 OkHttpClient（禁用缓存、仅 HEADERS 日志），
 * 避免大文件下载时 CacheInterceptor 写缓存 + HttpLoggingInterceptor 读 body 导致 SocketException。
 */
class Downloader(
    private val client: OkHttpClient,
) : DownloadRemoteDataSource {

    override suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): HttpResponse = withContext(Dispatchers.IO) {
        try {
            val okRequest = Request.Builder()
                .url(url)
                .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                .build()

            val response = client.newCall(okRequest).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return@withContext HttpResponse(
                        errorCode = "-1",
                        errorMsg = "http ${res.code} ${res.message.ifBlank { "request failed" }}",
                    )
                }
                val body = res.body
                val file = File(savePath, fileName)
                body.byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
                    }
                }
                HttpResponse(errorCode = "0", errorMsg = "success")
            }
        } catch (e: Exception) {
            HttpResponse(errorCode = "-1", errorMsg = e.message ?: e.javaClass.simpleName)
        }
    }
}
