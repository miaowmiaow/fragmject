package com.example.fragmject.core.network

import com.example.fragmject.core.data.contract.model.ApiCode
import com.example.fragmject.core.data.contract.model.DownloadFileResult
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 流式文件下载器。
 *
 * 替代原 CoroutineHttp.download：不走 Retrofit，直接用 OkHttp 流式写文件。
 * 使用下载专用 OkHttpClient（禁用缓存、不取响应体日志），
 * 避免大文件下载时 CacheInterceptor 写缓存 + 日志拦截器读 body 导致 SocketException。
 */
class OkHttpFileDownloader(
    private val client: OkHttpClient,
) : DownloadRemoteDataSource {

    override suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): HttpResponse = downloadWithHeaders(url, savePath, fileName, headers).response

    override suspend fun downloadWithHeaders(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): DownloadFileResult = withContext(Dispatchers.IO) {
        try {
            val okRequest = Request.Builder()
                .url(url)
                .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                .build()

            val response = client.newCall(okRequest).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return@withContext DownloadFileResult(
                        response = HttpResponse(
                            errorCode = ApiCode.CLIENT_FAILURE,
                            errorMsg = "http ${res.code} ${res.message.ifBlank { "request failed" }}",
                        ),
                    )
                }
                val body = res.body
                val file = File(savePath, fileName)
                body.byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
                    }
                }
                DownloadFileResult(
                    response = HttpResponse(errorCode = ApiCode.SUCCESS, errorMsg = "success"),
                    headers = res.headers.cacheRelevantHeaders(),
                )
            }
        } catch (e: CancellationException) {
            // 与 NetworkResultMapper / BasePagingSource 一致：取消信号必须原样抛出，
            // 否则「超时取消」会被误报为下载失败（WebResourceCacheManager 依赖此区分）
            throw e
        } catch (e: Exception) {
            DownloadFileResult(
                response = HttpResponse(
                    errorCode = ApiCode.CLIENT_FAILURE,
                    errorMsg = e.message ?: e.javaClass.simpleName,
                ),
            )
        }
    }

}

/**
 * 只取缓存决策需要的响应头。
 *
 * ## 为什么必须合并同名字段
 *
 * HTTP 允许把 `Cache-Control` / `Vary` 拆成多行，OkHttp 会把它们存成多个同名字段。
 * 而 `Response.header(name)` / `Headers[name]` 只返回**最后一个**值——若服务端发送：
 *
 * ```
 * Cache-Control: no-store
 * Cache-Control: max-age=600
 * ```
 *
 * 单值读取得到 `max-age=600`，缓存侧就会把「本应拒绝」的响应写进磁盘。
 * `Vary` 同理（`Vary: Cookie` 若不在最后一行就会被漏掉）。
 *
 * 因此这两个头用 [Headers.values] 全量取出并按 HTTP 语义以逗号合并；
 * `Content-Type` 保持单值（重复出现本身是非法响应，合并反而会破坏 mime 解析）。
 *
 * 注意这里**不取 `Content-Encoding`**：OkHttp 对 gzip 透明解压，落盘的是已解压的响应体，
 * 回放时再带 `Content-Encoding` 会让 WebView 二次解压（乱码 / 加载失败）。
 */
internal fun Headers.cacheRelevantHeaders(): Map<String, String> {
    val relevant = mutableMapOf<String, String>()
    MULTI_VALUE_CACHE_HEADERS.forEach { name ->
        val values = values(name)
        if (values.isNotEmpty()) relevant[name] = values.joinToString(", ")
    }
    this["Content-Type"]?.let { relevant["Content-Type"] = it }
    return relevant
}

/** 允许多行、且多行会被缓存侧当作**并列指令**解析的响应头。 */
private val MULTI_VALUE_CACHE_HEADERS = listOf("Cache-Control", "Vary")
