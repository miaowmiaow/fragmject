package com.example.fragment.library.base.http

import android.annotation.SuppressLint
import android.text.TextUtils
import com.example.fragment.library.base.utils.FileUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.net.URLEncoder
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.net.ssl.*
import kotlin.collections.HashMap
import kotlin.collections.set

suspend inline fun <reified T : HttpResponse> CoroutineScope.get(
    request: HttpRequest = HttpRequest()
): T {
    return SimpleHttp.instance().get(request, T::class.java)
}

suspend inline fun <reified T : HttpResponse> CoroutineScope.post(
    request: HttpRequest = HttpRequest()
): T {
    return SimpleHttp.instance().post(request, T::class.java)
}

suspend inline fun <reified T : HttpResponse> CoroutineScope.form(
    request: HttpRequest = HttpRequest()
): T {
    return SimpleHttp.instance().form(request, T::class.java)
}

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
        val keyManagers = HttpsUtil.prepareKeyManager(clientCertificate, clientCertificatePassword)
        val trustManager = HttpsUtil.prepareX509TrustManager(serverCertificates)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, arrayOf(trustManager), null)
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient()
            .newBuilder()
            .readTimeout(15000L, TimeUnit.MILLISECONDS)
            .writeTimeout(15000L, TimeUnit.MILLISECONDS)
            .connectTimeout(15000L, TimeUnit.MILLISECONDS)
            .hostnameVerifier { hostname, session ->
                if (hostNames != null) {
                    listOf(*hostNames!!)
                        .contains(hostname)
                } else HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
            }
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .addInterceptor(interceptor)
            .cache(Cache(FileUtils.getDir("http_cache"), 50 * 1024 * 1024))
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
            request.baseUrl(getRetrofit().baseUrl().toString())
            try {
                converter.converter(
                    getService().get(request.getUrl(), request.getHeader()), type
                )
            } catch (e: Exception) {
                val t = type.newInstance()
                t.errorMsg = e.message.toString()
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
                request.baseUrl(getRetrofit().baseUrl().toString())
                converter.converter(
                    getService().post(
                        request.getUrl(),
                        request.getHeader(),
                        request.getParam()
                    ), type
                )
            } catch (e: Exception) {
                val t = type.newInstance()
                t.errorMsg = e.message.toString()
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
                request.baseUrl(getRetrofit().baseUrl().toString())
                converter.converter(
                    getService().form(
                        request.getUrl(),
                        request.getHeader(),
                        body.build()
                    ), type
                )
            } catch (e: Exception) {
                val t = type.newInstance()
                t.errorMsg = e.message.toString()
                t
            }
        }
    }

    private fun guessMimeType(path: String): MediaType {
        val fileNameMap = URLConnection.getFileNameMap()
        var contentTypeFor = "application/octet-stream"
        try {
            fileNameMap.getContentTypeFor(URLEncoder.encode(path, "UTF-8")).also {
                if (!it.isNotBlank()) {
                    contentTypeFor = it
                }
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return contentTypeFor.toMediaType()
    }

    class GSonConverter : Converter {

        companion object {
            fun create(): GSonConverter {
                return GSonConverter()
            }
        }

        private val gSon = Gson()

        override fun <T> converter(responseBody: ResponseBody, type: Class<T>): T {
            val jsonReader = gSon.newJsonReader(responseBody.charStream())
            val adapter = gSon.getAdapter(type)
            return responseBody.use {
                adapter.read(jsonReader)
            }
        }
    }

    interface Converter {
        fun <T> converter(responseBody: ResponseBody, type: Class<T>): T
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
}

open class HttpRequest @JvmOverloads constructor(
    private var url: String = "",
    private var path: MutableMap<String, String> = HashMap(),
    private var query: MutableMap<String, String> = HashMap(),
    private var header: MutableMap<String, String> = HashMap(),
    private var params: MutableMap<String, String> = HashMap(),
    private var files: MutableMap<String, File> = HashMap()
) {
    companion object {
        private const val PARAM = "[a-zA-Z][a-zA-Z0-9_-]*"
        private var PARAM_URL_REGEX = Pattern.compile("\\{($PARAM)\\}")
    }

    private var baseUrl: String = ""

    fun baseUrl(baseUrl: String): HttpRequest {
        this.baseUrl = baseUrl
        return this
    }

    fun setUrl(url: String): HttpRequest {
        this.url = url
        return this
    }

    fun getUrl(): String {
        val m = PARAM_URL_REGEX.matcher(url)
        val patterns: MutableSet<String> = LinkedHashSet()
        while (m.find()) {
            patterns.add(m.group(1))
        }
        patterns.forEach {
            if (path.contains(it)) {
                url = url.replace("{$it}", path[it].toString())
            }
        }
        val urlStringBuilder = StringBuilder(url)
        val absoluteUrl = StringBuilder(baseUrl).append(url)
        if (!absoluteUrl.contains("?")) {
            urlStringBuilder.append("?")
        }
        if (!urlStringBuilder.endsWith("?")) {
            urlStringBuilder.append("&")
        }
        query.forEach { (key, value) ->
            urlStringBuilder.append(key).append("=").append(value).append("&")
        }
        urlStringBuilder.setLength(urlStringBuilder.length - 1)
        return urlStringBuilder.toString()
    }

    fun putPath(key: String, value: String): HttpRequest {
        if (!TextUtils.isEmpty(value)) {
            this.path[key] = value
        }
        return this
    }

    fun putQuery(key: String, value: String): HttpRequest {
        if (!TextUtils.isEmpty(value)) {
            this.query[key] = value
        }
        return this
    }

    fun putHeader(key: String, value: String): HttpRequest {
        this.header[key] = value
        return this
    }

    fun getHeader(): MutableMap<String, String> {
        return header
    }

    fun putParam(key: String, value: String): HttpRequest {
        this.params[key] = value
        return this
    }

    fun getParam(): MutableMap<String, String> {
        return params
    }

    fun putFile(key: String, file: File): HttpRequest {
        this.files[key] = file
        return this
    }

    fun getFile(): MutableMap<String, File> {
        return files
    }

}

open class HttpResponse @JvmOverloads constructor(
    var errorCode: String = "",
    var errorMsg: String = ""
)

object HttpsUtil {

    @Throws(Exception::class)
    fun prepareX509TrustManager(certificates: Array<InputStream>?): X509TrustManager {
        if (certificates == null) {
            return UnSafeTrustManager()
        }
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)
        for ((index, certificate) in certificates.withIndex()) {
            val certificateAlias = (index).toString()
            keyStore.setCertificateEntry(
                certificateAlias,
                certificateFactory.generateCertificate(certificate)
            )
            certificate.close()
        }
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        val trustManagers = trustManagerFactory.trustManagers
        return if (!trustManagers.isNullOrEmpty() && trustManagers[0] is X509TrustManager) {
            trustManagers[0] as X509TrustManager
        } else {
            UnSafeTrustManager()
        }
    }

    @Throws(Exception::class)
    fun prepareKeyManager(bksFile: InputStream?, password: String?): Array<KeyManager>? {
        if (bksFile == null || password == null) return null
        val clientKeyStore = KeyStore.getInstance("PKCS12")
        clientKeyStore.load(bksFile, password.toCharArray())
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(clientKeyStore, password.toCharArray())
        return keyManagerFactory.keyManagers
    }

    @SuppressLint("TrustAllX509TrustManager")
    class UnSafeTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
}