package com.example.fragment.library.base.http

import kotlinx.coroutines.CoroutineScope
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

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
        private var instance: SimpleHttp? = null

        fun instance() = instance ?: synchronized(this) {
            instance ?: SimpleHttp().also { instance = it }
        }

        @JvmStatic
        fun setBaseUrl(baseUrl: String? = null): Companion {
            instance().baseUrl.clear().append(baseUrl)
            instance().httpClient = null
            instance().retrofit = null
            return this
        }

        @JvmStatic
        fun setHttpClient(httpClient: OkHttpClient? = null): Companion {
            instance().httpClient = httpClient
            return this
        }

        @JvmStatic
        fun setConverter(converter: Converter): Companion {
            instance().converter = converter
            return this
        }
    }

    private var retrofit: Retrofit? = null
    private var service: ApiService? = null
    private var httpClient: OkHttpClient? = null
    private var baseUrl = StringBuilder()
    private var hostNames: Array<String>? = null
    private var clientCertificate: InputStream? = null
    private var serverCertificates: Array<InputStream>? = null
    private var clientCertificatePassword: String? = null
    private var converter: Converter = GSonConverter.create()

    private fun getHttpClient(): OkHttpClient {
        return httpClient ?: buildHttpClient().also { httpClient = it }
    }

    private fun buildHttpClient(): OkHttpClient {
        val keyManagers =
            HttpsHelper.prepareKeyManager(clientCertificate, clientCertificatePassword)
        val trustManager = HttpsHelper.prepareX509TrustManager(serverCertificates)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, arrayOf(trustManager), null)
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient()
            .newBuilder()
            .readTimeout(15000L, TimeUnit.MILLISECONDS)
            .writeTimeout(15000L, TimeUnit.MILLISECONDS)
            .connectTimeout(15000L, TimeUnit.MILLISECONDS)
            .cookieJar(CookieJar())
            .hostnameVerifier { hostname, session ->
                if (hostNames != null) {
                    listOf(*hostNames!!).contains(hostname)
                } else {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            }
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .addNetworkInterceptor(interceptor)
            .build()
    }

    private fun getRetrofit() = retrofit ?: synchronized(this) {
        retrofit ?: buildRetrofit().also { retrofit = it }
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl.toString())
            .client(getHttpClient())
            .build()
    }

    private fun getService() = service ?: synchronized(this) {
        service ?: getRetrofit().create(ApiService::class.java).also { service = it }
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
                    request.getUrl(baseUrl.toString()),
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
                    request.getUrl(baseUrl.toString()),
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
                    request.getUrl(baseUrl.toString()),
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

