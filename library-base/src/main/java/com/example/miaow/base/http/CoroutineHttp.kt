package com.example.miaow.base.http

import android.content.Context
import android.util.Log
import com.example.miaow.base.debug.DebugBridge
import com.example.miaow.base.utils.FileUtil
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File

private const val TAG = "CoroutineHttp"

/**
 * get请求
 * @param init  http请求体
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.get(
    noinline init: HttpRequest.() -> Unit
): T {
    return CoroutineHttp.getInstance().get(init, T::class.java)
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
suspend inline fun <reified T : HttpResponse> CoroutineScope.post(
    noinline init: HttpRequest.() -> Unit
): T {
    return CoroutineHttp.getInstance().post(init, T::class.java)
}

/**
 * form请求
 * @param init  http请求体
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.form(
    noinline init: HttpRequest.() -> Unit
): T {
    return CoroutineHttp.getInstance().form(init, T::class.java)
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

fun Context.setBaseUrl(baseUrl: String) {
    CoroutineHttp.getInstance().setBaseUrl(baseUrl)
}

fun Context.setHttpClient(client: OkHttpClient) {
    CoroutineHttp.getInstance().setHttpClient(client)
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
    private lateinit var client: OkHttpClient
    private var retrofit: Retrofit? = null
    private var service: ApiService? = null
    private var converter: Converter? = null

    fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    fun setHttpClient(client: OkHttpClient) {
        this.client = client
    }

    private fun getRetrofit(): Retrofit {
        return retrofit ?: Retrofit.Builder().baseUrl(baseUrl).client(client).build().also {
            retrofit = it
        }
    }

    private fun getService(): ApiService {
        return service ?: getRetrofit().create(ApiService::class.java).also { service = it }
    }

    private fun getConverter(): Converter {
        return converter ?: GSonConverter.create().also { converter = it }
    }

    suspend fun <T : HttpResponse> get(
        init: HttpRequest.() -> Unit,
        type: Class<T>,
    ): T {
        val request = HttpRequest().apply(init)
        return try {
            getService().get(request.getUrl(baseUrl), request.getHeader()).body()?.let { body ->
                getConverter().converter(body, type).apply { setRequestTime(request.time) }
            } ?: buildResponse("-1", "response body is null", type)
        } catch (e: Exception) {
            Log.e(TAG, "GET ${request.getUrl(baseUrl)} failed", e)
            fallbackResponse(request, type, e)
        }
    }

    suspend fun <T : HttpResponse> post(
        init: HttpRequest.() -> Unit,
        type: Class<T>,
    ): T {
        val request = HttpRequest().apply(init)
        return try {
            getService().post(
                request.getUrl(baseUrl),
                request.getHeader(),
                request.getParam()
            ).body()?.let { body ->
                getConverter().converter(body, type).apply { setRequestTime(request.time) }
            } ?: buildResponse("-1", "response body is null", type)
        } catch (e: Exception) {
            Log.e(TAG, "POST ${request.getUrl(baseUrl)} failed", e)
            fallbackResponse(request, type, e)
        }
    }

    suspend fun <T : HttpResponse> form(
        init: HttpRequest.() -> Unit,
        type: Class<T>,
    ): T {
        val request = HttpRequest().apply(init)
        return try {
            getService().form(
                request.getUrl(baseUrl),
                request.getHeader(),
                request.getMultipartBody()
            ).body()?.let { body ->
                getConverter().converter(body, type).apply { setRequestTime(request.time) }
            } ?: buildResponse("-1", "response body is null", type)
        } catch (e: Exception) {
            Log.e(TAG, "FORM ${request.getUrl(baseUrl)} failed", e)
            buildResponse("-1", e.message ?: e.javaClass.simpleName, type)
        }
    }

    suspend fun download(
        savePath: String,
        fileName: String,
        init: HttpRequest.() -> Unit
    ): HttpResponse {
        val request = HttpRequest().apply(init)
        return try {
            val response = getService().get(request.getUrl(), request.getHeader())
            if (response.isSuccessful) {
                val file = File(savePath, fileName)
                response.body()?.byteStream()?.use { inputStream ->
                    file.writeBytes(inputStream.readBytes())
                }
            }
            buildResponse("0", "success", HttpResponse::class.java)
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
            ""
        }
    }

    /**
     * 网络异常时的兜底逻辑：
     * - Debug 包尝试读取 assets/json/ 下的预置数据，方便离线调试与示例运行；
     * - Release 包不再读取本地 assets，直接返回错误响应，避免线上隐式行为。
     */
    private fun <T : HttpResponse> fallbackResponse(
        request: HttpRequest,
        type: Class<T>,
        e: Exception
    ): T {
        if (DebugBridge.allowAssetsFallback) {
            val jsonName = request.getUrl(baseUrl).replace("/", "-").replace("?", "_")
            val json = FileUtil.readAssetString("json/${jsonName}.json")
            if (json.isNotBlank()) {
                return getConverter().fromJson(json, type)
            }
        }
        return buildResponse("-1", e.message ?: e.javaClass.simpleName, type)
    }

    /**
     * 通过 JsonObject 构建错误响应，避免使用字符串拼接造成的 JSON 注入 / 非法 JSON 风险
     * （之前的实现仅替换双引号，遗漏了反斜杠、换行等其他特殊字符）。
     */
    private fun <T : HttpResponse> buildResponse(code: String, msg: String, type: Class<T>): T {
        val obj = JsonObject().apply {
            addProperty("errorCode", code)
            addProperty("errorMsg", msg)
        }
        return getConverter().fromJson(obj.toString(), type)
    }

    interface Converter {
        fun <T> converter(responseBody: ResponseBody, type: Class<T>): T

        @Throws(Exception::class)
        fun <T> fromJson(json: String, classOfT: Class<T>): T
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

