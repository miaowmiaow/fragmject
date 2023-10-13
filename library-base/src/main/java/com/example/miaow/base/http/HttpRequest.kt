package com.example.miaow.base.http

import android.text.TextUtils
import android.util.Log
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLConnection
import java.net.URLEncoder
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * http请求体
 */
open class HttpRequest @JvmOverloads constructor(
    private var url: String = "",
    private var path: MutableMap<String, String> = HashMap(),
    private var query: MutableMap<String, String> = HashMap(),
    private var header: MutableMap<String, String> = HashMap(),
    private var params: MutableMap<String, String> = HashMap(),
    private var files: MutableMap<String, File> = HashMap()
) {

    var time = 0L

    init {
        time = System.currentTimeMillis()
    }

    fun setUrl(url: String): HttpRequest {
        this.url = url
        return this
    }

    fun getUrl(baseUrl: String? = null): String {
        val matcher = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_-]*)\\}").matcher(url)
        val patterns: MutableSet<String> = LinkedHashSet()
        while (matcher.find()) {
            matcher.group(1)?.let {
                patterns.add(it)
            }
        }
        patterns.forEach {
            if (path.contains(it)) {
                url = url.replace("{$it}", path[it].toString())
            }
        }
        val relativeUrl = StringBuilder(url)
        if (query.isNotEmpty()) {
            val absoluteUrl = StringBuilder()
            baseUrl?.apply { absoluteUrl.append(this) }
            absoluteUrl.append(url)
            if (!absoluteUrl.contains("?")) {
                relativeUrl.append("?")
            }
            if (!relativeUrl.endsWith("?")) {
                relativeUrl.append("&")
            }
            query.forEach { (key, value) ->
                relativeUrl.append(key).append("=").append(value).append("&")
            }
            relativeUrl.setLength(relativeUrl.length - 1)
        }
        return relativeUrl.toString()
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

    fun getMultipartBody(): MultipartBody {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        getParam().forEach {
            body.addFormDataPart(it.key, it.value)
        }
        getFile().forEach {
            it.value.apply {
                val lastIndex = absolutePath.lastIndexOf("/")
                val fileName = absolutePath.substring(lastIndex)
                if (fileName.isNotEmpty()) {
                    val requestBody = asRequestBody(guessMimeType(fileName))
                    body.addFormDataPart(it.key, fileName, requestBody)
                }
            }
        }
        return body.build()
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
            Log.e(this.javaClass.name, e.message.toString())
        }
        return contentTypeFor.toMediaType()
    }

}