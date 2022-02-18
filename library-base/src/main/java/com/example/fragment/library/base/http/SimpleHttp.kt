package com.example.fragment.library.base.http

import android.content.ContextWrapper
import kotlinx.coroutines.CoroutineScope
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File

/**
 * get请求
 * @param request  http请求体
 * @param progress 进度回调方法
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.get(
    request: HttpRequest,
    noinline progress: ((Double) -> Unit)? = null
): T {
    return SimpleHttp.instance().get(request, T::class.java, progress)
}

/**
 * post请求
 * @param request  http请求体
 * @param progress 进度回调方法
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.post(
    request: HttpRequest,
    noinline progress: ((Double) -> Unit)? = null
): T {
    return SimpleHttp.instance().post(request, T::class.java, progress)
}

/**
 * form请求
 * @param request  http请求体
 * @param progress 进度回调方法
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.form(
    request: HttpRequest,
    noinline progress: ((Double) -> Unit)? = null
): T {
    return SimpleHttp.instance().form(request, T::class.java, progress)
}

/**
 * download请求
 * @param request  http请求体
 * @param progress 进度回调方法
 */
suspend inline fun CoroutineScope.download(
    request: HttpRequest,
    filePathName: String,
    noinline progress: ((Double) -> Unit)? = null
): HttpResponse {
    return SimpleHttp.instance().download(request, filePathName, progress)
}

fun ContextWrapper.setBaseUrl(baseUrl: String) {
    SimpleHttp.instance().setBaseUrl(baseUrl)
}

fun ContextWrapper.setHttpClient(client: OkHttpClient) {
    SimpleHttp.instance().setHttpClient(client)
}

/**
 * retrofit + coroutines 封装的Http工具类
 */
class SimpleHttp private constructor() {

    companion object {

        @Volatile
        private var INSTANCE: SimpleHttp? = null

        fun instance() = INSTANCE ?: synchronized(SimpleHttp::class.java) {
            INSTANCE ?: SimpleHttp().also { INSTANCE = it }
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
        return converter ?: GSonConverter.create()
    }

    suspend fun <T : HttpResponse> get(
        request: HttpRequest,
        type: Class<T>,
        progress: ((Double) -> Unit)? = null
    ): T {
        return try {
            progress?.invoke(0.0)
            val responseBody = getService().get(request.getUrl(baseUrl), request.getHeader())
            getConverter().converter(responseBody, type)
        } catch (e: Exception) {
            type.newInstance().apply {
                errorMsg = e.message.toString()
            }
        } finally {
            progress?.invoke(1.0)
        }
    }

    suspend fun <T : HttpResponse> post(
        request: HttpRequest,
        type: Class<T>,
        progress: ((Double) -> Unit)? = null
    ): T {
        return try {
            progress?.invoke(0.0)
            val responseBody = getService().post(
                request.getUrl(baseUrl),
                request.getHeader(),
                request.getParam()
            )
            getConverter().converter(responseBody, type)
        } catch (e: Exception) {
            type.newInstance().apply {
                errorMsg = e.message.toString()
            }
        } finally {
            progress?.invoke(1.0)
        }
    }

    suspend fun <T : HttpResponse> form(
        request: HttpRequest,
        type: Class<T>,
        progress: ((Double) -> Unit)? = null
    ): T {
        return try {
            progress?.invoke(0.0)
            val responseBody = getService().form(
                request.getUrl(baseUrl),
                request.getHeader(),
                request.getMultipartBody()
            )
            getConverter().converter(responseBody, type)
        } catch (e: Exception) {
            type.newInstance().apply {
                errorMsg = e.message.toString()
            }
        } finally {
            progress?.invoke(1.0)
        }
    }

    suspend fun download(
        request: HttpRequest,
        filePathName: String,
        progress: ((Double) -> Unit)? = null
    ): HttpResponse {
        return try {
            progress?.invoke(0.0)
            getService().download(request.getUrl(), request.getHeader()).use {
                val file = File(filePathName)
                it.byteStream().use { inputStream ->
                    file.writeBytes(inputStream.readBytes())
                }
            }
            HttpResponse("0", "success")
        } catch (e: Exception) {
            HttpResponse("-1", e.message.toString())
        } finally {
            progress?.invoke(1.0)
        }
    }

    interface Converter {
        fun <T> converter(responseBody: ResponseBody, type: Class<T>): T
    }

}

interface ApiService {

    @GET
    suspend fun download(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>
    ): ResponseBody

    @POST
    suspend fun form(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>,
        @Body body: MultipartBody
    ): ResponseBody

    @FormUrlEncoded
    @POST
    suspend fun post(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>,
        @FieldMap params: Map<String, String>
    ): ResponseBody

    @GET
    suspend fun get(
        @Url url: String = "",
        @HeaderMap header: Map<String, String>
    ): ResponseBody
}

