package com.example.fragment.library.base.http

import kotlinx.coroutines.CoroutineScope
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.net.URLEncoder

/**
 * get请求
 * @param request  http请求体
 * @param progress 进度回调方法
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.get(
    request: HttpRequest = HttpRequest(),
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
    request: HttpRequest = HttpRequest(),
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
    request: HttpRequest = HttpRequest(),
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
    request: HttpRequest = HttpRequest(),
    filePathName: String,
    noinline progress: ((Double) -> Unit)? = null
): HttpResponse {
    return SimpleHttp.instance().download(request, filePathName, progress)
}

/**
 * retrofit + coroutines 封装的Http工具类
 */
class SimpleHttp private constructor() {

    companion object {

        @Volatile
        private var INSTANCE: SimpleHttp? = null

        fun instance() = INSTANCE ?: synchronized(this) {
            INSTANCE ?: SimpleHttp().also { INSTANCE = it }
        }

        @JvmStatic
        fun setBaseUrl(baseUrl: String): Companion {
            instance().setBaseUrl(baseUrl)
            return this
        }

        @JvmStatic
        fun setHttpClient(client: OkHttpClient): Companion {
            instance().setHttpClient(client)
            return this
        }

        @JvmStatic
        fun setConverter(converter: Converter): Companion {
            instance().setConverter(converter)
            return this
        }
    }

    private lateinit var baseUrl: String
    private lateinit var client: OkHttpClient
    private lateinit var converter: Converter
    private var retrofit: Retrofit? = null
    private var service: ApiService? = null

    private fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    private fun setHttpClient(client: OkHttpClient) {
        this.client = client
    }

    private fun setConverter(converter: Converter) {
        this.converter = converter
    }

    private fun getRetrofit(): Retrofit {
        return retrofit ?: Retrofit.Builder().baseUrl(baseUrl).client(client).build().also {
            retrofit = it
        }
    }

    private fun getService(): ApiService {
        return service ?: getRetrofit().create(ApiService::class.java).also {
            service = it
        }
    }

    suspend fun <T : HttpResponse> get(
        request: HttpRequest = HttpRequest(),
        type: Class<T>,
        progress: ((Double) -> Unit)? = null
    ): T {
        return try {
            progress?.invoke(0.0)
            converter.converter(
                getService().get(
                    request.getUrl(baseUrl),
                    request.getHeader()
                ),
                type
            )
        } catch (e: Exception) {
            val msg = e.message.toString()
            val t = type.newInstance()
            t.errorMsg = msg
            t
        } finally {
            progress?.invoke(1.0)
        }
    }

    suspend fun <T : HttpResponse> post(
        request: HttpRequest = HttpRequest(),
        type: Class<T>,
        progress: ((Double) -> Unit)? = null
    ): T {
        return try {
            progress?.invoke(0.0)
            converter.converter(
                getService().post(
                    request.getUrl(baseUrl),
                    request.getHeader(),
                    request.getParam()
                ),
                type
            )
        } catch (e: Exception) {
            val msg = e.message.toString()
            val t = type.newInstance()
            t.errorMsg = msg
            t
        } finally {
            progress?.invoke(1.0)
        }
    }

    suspend fun <T : HttpResponse> form(
        request: HttpRequest = HttpRequest(),
        type: Class<T>,
        progress: ((Double) -> Unit)? = null
    ): T {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        request.getParam().forEach { map ->
            body.addFormDataPart(map.key, map.value)
        }
        request.getFile().forEach { map ->
            map.value.apply {
                val lastIndex = absolutePath.lastIndexOf("/")
                val fileName = absolutePath.substring(lastIndex)
                if (fileName.isNotEmpty()) {
                    body.addFormDataPart(
                        map.key,
                        fileName,
                        asRequestBody(guessMimeType(fileName))
                    )
                }
            }
        }
        return try {
            progress?.invoke(0.0)
            converter.converter(
                getService().form(
                    request.getUrl(baseUrl),
                    request.getHeader(),
                    body.build()
                ),
                type
            )
        } catch (e: Exception) {
            val msg = e.message.toString()
            val t = type.newInstance()
            t.errorMsg = msg
            t
        } finally {
            progress?.invoke(1.0)
        }
    }

    suspend fun download(
        request: HttpRequest = HttpRequest(),
        filePathName: String,
        progress: ((Double) -> Unit)? = null
    ): HttpResponse {
        return try {
            progress?.invoke(0.0)
            getService().download(request.getUrl(), request.getHeader()).use { body ->
                val file = File(filePathName)
                body.byteStream().use { inputStream ->
                    file.writeBytes(inputStream.readBytes())
                }
            }
            HttpResponse("0", "success")
        } catch (e: Exception) {
            val msg = e.message.toString()
            HttpResponse("-1", msg)
        } finally {
            progress?.invoke(1.0)
        }
    }

    private fun guessMimeType(path: String): MediaType {
        val fileNameMap = URLConnection.getFileNameMap()
        var contentTypeFor = "application/octet-stream"
        try {
            fileNameMap.getContentTypeFor(URLEncoder.encode(path, "UTF-8")).also {
                if (it.isBlank()) {
                    contentTypeFor = it
                }
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return contentTypeFor.toMediaType()
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

