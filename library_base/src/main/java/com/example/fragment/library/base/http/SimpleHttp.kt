package com.example.fragment.library.base.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

/**
 * get请求
 *      通过HttpRequest构建请求体
 *      通过泛型确定返回值
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.get(
    request: HttpRequest = HttpRequest()
): T {
    return SimpleHttp.instance().get(request, T::class.java)
}

/**
 * post请求
 *      通过HttpRequest构建请求体
 *      通过泛型确定返回值
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.post(
    request: HttpRequest = HttpRequest()
): T {
    return SimpleHttp.instance().post(request, T::class.java)
}

/**
 * form请求
 *      通过HttpRequest构建请求体
 *      通过泛型确定返回值
 */
suspend inline fun <reified T : HttpResponse> CoroutineScope.form(
    request: HttpRequest = HttpRequest()
): T {
    return SimpleHttp.instance().form(request, T::class.java)
}

/**
 * 基于个人习惯的Retrofit+协程的简单封装
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
            .cookieJar(SimpleCookieJar())
            .hostnameVerifier { hostname, session ->
                if (hostNames != null) {
                    listOf(*hostNames!!)
                        .contains(hostname)
                } else HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
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
        type: Class<T>
    ): T {
        return withContext(Dispatchers.IO) {
            try {
                request.setBaseUrl(baseUrl.toString())
                converter.converter(
                    getService().get(request.getUrl(), request.getHeader()), type
                )
            } catch (e: Exception) {
                val msg = e.message.toString()
                val t = type.newInstance()
                t.errorMsg = msg
                t
            }
        }
    }

    suspend fun <T : HttpResponse> post(
        request: HttpRequest = HttpRequest(),
        type: Class<T>
    ): T {
        return withContext(Dispatchers.IO) {
            try {
                request.setBaseUrl(baseUrl.toString())
                converter.converter(
                    getService().post(
                        request.getUrl(),
                        request.getHeader(),
                        request.getParam()
                    ), type
                )
            } catch (e: Exception) {
                val msg = e.message.toString()
                val t = type.newInstance()
                t.errorMsg = msg
                t
            }
        }
    }

    suspend fun <T : HttpResponse> form(
        request: HttpRequest = HttpRequest(),
        type: Class<T>
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
        return withContext(Dispatchers.IO) {
            try {
                request.setBaseUrl(baseUrl.toString())
                converter.converter(
                    getService().form(
                        request.getUrl(),
                        request.getHeader(),
                        body.build()
                    ), type
                )
            } catch (e: Exception) {
                val msg = e.message.toString()
                val t = type.newInstance()
                t.errorMsg = msg
                t
            }
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

