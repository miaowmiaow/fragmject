package com.example.fragmject.core.network.http

import android.util.Log
import com.example.fragmject.core.network.debug.DebugBridge
import com.example.fragmject.core.network.utils.FileUtil
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import java.lang.reflect.Type

private const val TAG = "CoroutineHttp"

/**
 * get请求
 * @param init  http请求体
 */
suspend inline fun <reified T> CoroutineScope.get(
    noinline init: HttpRequest.() -> Unit
): T {
    return CoroutineHttp.getInstance().get(init, object : TypeToken<T>() {}.type)
}

suspend inline fun CoroutineScope.string(
    noinline init: HttpRequest.() -> Unit
): String {
    return CoroutineHttp.getInstance().string(init)
}

/**
 * post请求
 * @param init  http请求体
 */
suspend inline fun <reified T> CoroutineScope.post(
    noinline init: HttpRequest.() -> Unit
): T {
    return CoroutineHttp.getInstance().post(init, object : TypeToken<T>() {}.type)
}

/**
 * form请求
 * @param init  http请求体
 */
suspend inline fun <reified T> CoroutineScope.form(
    noinline init: HttpRequest.() -> Unit
): T {
    return CoroutineHttp.getInstance().form(init, object : TypeToken<T>() {}.type)
}

/**
 * download请求
 * @param savePath 保存路径
 * @param fileName 文件名称
 * @param init  http请求体
 */
suspend inline fun CoroutineScope.download(
    savePath: String,
    fileName: String,
    noinline init: HttpRequest.() -> Unit
): HttpResponse {
    return CoroutineHttp.getInstance().download(savePath, fileName, init)
}

fun setBaseUrl(baseUrl: String) {
    CoroutineHttp.getInstance().setBaseUrl(baseUrl)
}

fun setHttpClient(client: OkHttpClient) {
    CoroutineHttp.getInstance().setHttpClient(client)
}

/**
 * 懒加载版本的 setHttpClient：provider 仅在首次发起请求时被调用，
 * 避免在 Application.onCreate 阶段在主线程同步创建 OkHttpClient，减少冷启动耗时。
 */
fun setHttpClientLazy(provider: () -> OkHttpClient) {
    CoroutineHttp.getInstance().setClientProvider(provider)
}

/**
 * retrofit + coroutines 封装的Http工具类
 */
class CoroutineHttp private constructor() {

    companion object {

        @Volatile
        private var INSTANCE: CoroutineHttp? = null

        fun getInstance() = INSTANCE ?: synchronized(CoroutineHttp::class.java) {
            INSTANCE ?: CoroutineHttp().also { INSTANCE = it }
        }

    }

    private lateinit var baseUrl: String
    private var client: OkHttpClient? = null
    private var clientProvider: (() -> OkHttpClient)? = null
    private var retrofit: Retrofit? = null
    private var service: ApiService? = null
    private var converter: Converter? = null

    fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    fun setHttpClient(client: OkHttpClient) {
        this.client = client
        // 显式设置后丢弃之前可能存在的 provider，避免两者同时生效造成誓言不一致。
        this.clientProvider = null
        // baseUrl / client 变更后，原有 retrofit 已失效，重置以保证下次调用重建
        retrofit = null
        service = null
    }

    /**
     * 与 [setHttpClient] 二选一。provider 仅在第一次需要 OkHttpClient 时调用，
     * 让冷启动阶段不再同步构造 OkHttpClient（OkHttp + Cache + 拦截器 几十毫秒级的耗时）。
     */
    fun setClientProvider(provider: () -> OkHttpClient) {
        this.clientProvider = provider
    }

    /**
     * 获取当前 OkHttpClient：优先读显式设置的实例，其次从 provider 需要时创建并缓存。
     */
    @Synchronized
    private fun obtainClient(): OkHttpClient {
        client?.let { return it }
        val provider = clientProvider
            ?: error("OkHttpClient not configured: call setHttpClient or setHttpClientLazy first")
        return provider().also { client = it }
    }

    private fun getRetrofit(): Retrofit {
        return retrofit ?: Retrofit.Builder().baseUrl(baseUrl).client(obtainClient()).build().also {
            retrofit = it
        }
    }

    private fun getService(): ApiService {
        return service ?: getRetrofit().create(ApiService::class.java).also { service = it }
    }

    private fun getConverter(): Converter {
        return converter ?: GSonConverter.create().also { converter = it }
    }

    suspend fun <T> get(
        init: HttpRequest.() -> Unit,
        typeOfT: Type,
    ): T = get(HttpRequest().apply(init), typeOfT)

    /**
     * 接收已构造好的 [HttpRequest] 的重载，便于上层（如 SWR 缓存算子）先从 request 派生
     * cacheKey、再用同一个 request 发起网络请求，避免对 `init: HttpRequest.() -> Unit`
     * 反复 apply 造成的重复构造与 [HttpRequest.time] 漂移。
     */
    suspend fun <T> get(
        request: HttpRequest,
        typeOfT: Type,
    ): T {
        return try {
            getService().get(request.getUrl(baseUrl), request.getHeader()).body()?.let { body ->
                getConverter().converter<T>(body, typeOfT).apply {
                    if(this is HttpResponse){
                        setRequestTime(request.time)
                    }
                }
            } ?: buildResponse<T>("-1", "response body is null", typeOfT)
        } catch (e: Exception) {
            Log.e(TAG, "GET ${request.getUrl(baseUrl)} failed", e)
            fallbackResponse(request, typeOfT, e)
        }
    }

    suspend fun <T> post(
        init: HttpRequest.() -> Unit,
        typeOfT: Type,
    ): T = post(HttpRequest().apply(init), typeOfT)

    /** 与 [get] 同名重载语义一致：复用上层已构造的 request，避免双 apply。 */
    suspend fun <T> post(
        request: HttpRequest,
        typeOfT: Type,
    ): T {
        return try {
            getService().post(
                request.getUrl(baseUrl),
                request.getHeader(),
                request.getParam()
            ).body()?.let { body ->
                getConverter().converter<T>(body, typeOfT).apply {
                    if(this is HttpResponse){
                        setRequestTime(request.time)
                    }
                }
            } ?: buildResponse<T>("-1", "response body is null", typeOfT)
        } catch (e: Exception) {
            Log.e(TAG, "POST ${request.getUrl(baseUrl)} failed", e)
            fallbackResponse(request, typeOfT, e)
        }
    }

    suspend fun <T> form(
        init: HttpRequest.() -> Unit,
        typeOfT: Type,
    ): T {
        val request = HttpRequest().apply(init)
        return try {
            getService().form(
                request.getUrl(baseUrl),
                request.getHeader(),
                request.getMultipartBody()
            ).body()?.let { body ->
                getConverter().converter<T>(body, typeOfT).apply {
                    if(this is HttpResponse){
                        setRequestTime(request.time)
                    }
                }
            } ?: buildResponse<T>("-1", "response body is null", typeOfT)
        } catch (e: Exception) {
            Log.e(TAG, "FORM ${request.getUrl(baseUrl)} failed", e)
            buildResponse<T>("-1", e.message ?: e.javaClass.simpleName, typeOfT)
        }
    }

    suspend fun download(
        savePath: String,
        fileName: String,
        init: HttpRequest.() -> Unit
    ): HttpResponse {
        val request = HttpRequest().apply(init)
        return try {
            // 下载专用 OkHttpClient：克隆现有 client、清除 BODY 日志拦截器、禁用缓存，
            // 避免大文件下载时 CacheInterceptor 写缓存 + HttpLoggingInterceptor 读 body 导致 SocketException
            val downloadClient = obtainClient().newBuilder().apply {
                networkInterceptors().removeAll { it is HttpLoggingInterceptor }
                addNetworkInterceptor(
                    HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
                )
                cache(null)
            }.build()

            val okRequest = okhttp3.Request.Builder()
                .url(request.getUrl())
                .apply { request.getHeader().forEach { (k, v) -> addHeader(k, v) } }
                .build()

            val response = downloadClient.newCall(okRequest).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return buildResponse(
                        "-1",
                        "http ${res.code} ${res.message.ifBlank { "request failed" }}",
                        HttpResponse::class.java
                    )
                }
                val body = res.body
                val file = File(savePath, fileName)
                body.byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
                    }
                }
                buildResponse("0", "success", HttpResponse::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "DOWNLOAD ${request.getUrl()} failed", e)
            buildResponse("-1", e.message ?: e.javaClass.simpleName, HttpResponse::class.java)
        }
    }

    suspend fun string(
        init: HttpRequest.() -> Unit,
    ): String {
        val request = HttpRequest().apply(init)
        return try {
            getService().get(
                request.getUrl(baseUrl),
                request.getHeader()
            ).body()?.string().orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "STRING ${request.getUrl(baseUrl)} failed", e)
            "__HTTP_ERROR__:${e.message ?: e.javaClass.simpleName}"
        }
    }

    /**
     * 网络异常时的兜底逻辑：
     * - Debug 包尝试读取 assets/json/ 下的预置数据，方便离线调试与示例运行；
     * - Release 包不再读取本地 assets，直接返回错误响应，避免线上隐式行为。
     */
    private fun <T> fallbackResponse(
        request: HttpRequest,
        typeOfT: Type,
        e: Exception
    ): T {
        if (DebugBridge.allowAssetsFallback) {
            val jsonName = request.getUrl(baseUrl).replace("/", "-").replace("?", "_")
            val json = FileUtil.readAssetString("json/${jsonName}.json")
            if (json.isNotBlank()) {
                return getConverter().fromJson<T>(json, typeOfT)
            }
        }
        return buildResponse<T>("-1", e.message ?: e.javaClass.simpleName, typeOfT)
    }

    /**
     * 通过 JsonObject 构建错误响应，避免使用字符串拼接造成的 JSON 注入 / 非法 JSON 风险
     * （之前的实现仅替换双引号，遗漏了反斜杠、换行等其他特殊字符）。
     */
    private fun <T> buildResponse(code: String, msg: String, typeOfT: Type,): T {
        val obj = JsonObject().apply {
            addProperty("errorCode", code)
            addProperty("errorMsg", msg)
        }
        return getConverter().fromJson<T>(obj.toString(), typeOfT)
    }

    interface Converter {
        fun <T> converter(responseBody: ResponseBody, typeOfT: Type): T

        @Throws(Exception::class)
        fun <T> fromJson(json: String, typeOfT: Type): T
    }

}

interface ApiService {

    @POST
    suspend fun form(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>,
        @Body body: MultipartBody
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun post(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>,
        @FieldMap params: Map<String, String>
    ): Response<ResponseBody>

    @GET
    suspend fun get(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>
    ): Response<ResponseBody>
}