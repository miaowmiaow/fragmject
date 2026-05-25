package com.example.miaow.base.http

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
    private var header: MutableMap<String, String> = HashMap(),
    private var path: MutableMap<String, String> = HashMap(),
    private var query: MutableMap<String, String> = HashMap(),
    private var params: MutableMap<String, String> = HashMap(),
    private var files: MutableMap<String, File> = HashMap()
) {

    var time = System.currentTimeMillis()

    fun setUrl(url: String): HttpRequest {
        this.url = url
        return this
    }

    fun getUrl(baseUrl: String? = null): String {
        // 仅在局部变量上做替换，避免反复调用 getUrl 时改写成员变量造成的累计副作用
        var resolvedUrl = url
        val matcher = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_-]*)\\}").matcher(resolvedUrl)
        val patterns: MutableSet<String> = LinkedHashSet()
        while (matcher.find()) {
            matcher.group(1)?.let {
                patterns.add(it)
            }
        }
        patterns.forEach {
            if (path.contains(it)) {
                resolvedUrl = resolvedUrl.replace("{$it}", path[it].toString())
            }
        }
        val relativeUrl = StringBuilder(resolvedUrl)
        if (query.isNotEmpty()) {
            val absoluteUrl = StringBuilder()
            baseUrl?.apply { absoluteUrl.append(this) }
            absoluteUrl.append(resolvedUrl)
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
        if (value.isNotEmpty()) {
            this.path[key] = value
        }
        return this
    }

    fun putPath(from: Map<String, String>): HttpRequest {
        this.path.putAll(from)
        return this
    }

    fun putQuery(key: String, value: String): HttpRequest {
        if (value.isNotEmpty()) {
            this.query[key] = value
        }
        return this
    }

    fun putQuery(from: Map<String, String>): HttpRequest {
        this.query.putAll(from)
        return this
    }

    fun putHeader(key: String, value: String): HttpRequest {
        this.header[key] = value
        return this
    }

    fun putHeader(from: Map<String, String>): HttpRequest {
        this.header.putAll(from)
        return this
    }

    fun getHeader(): MutableMap<String, String> {
        return header
    }

    fun putParam(key: String, value: String): HttpRequest {
        this.params[key] = value
        return this
    }

    fun putParam(from: Map<String, String>): HttpRequest {
        this.params.putAll(from)
        return this
    }

    fun getParam(): MutableMap<String, String> {
        return params
    }

    fun putFile(key: String, file: File): HttpRequest {
        this.files[key] = file
        return this
    }

    fun putFile(from: Map<String, File>): HttpRequest {
        this.files.putAll(from)
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
            val guessed = fileNameMap.getContentTypeFor(URLEncoder.encode(path, "UTF-8"))
            // 之前的判断写反了：仅当 guessed 非空时才覆盖默认值
            if (!guessed.isNullOrBlank()) {
                contentTypeFor = guessed
            }
        } catch (e: UnsupportedEncodingException) {
            Log.e(this.javaClass.name, "guessMimeType failed for $path", e)
        }
        return contentTypeFor.toMediaType()
    }

}