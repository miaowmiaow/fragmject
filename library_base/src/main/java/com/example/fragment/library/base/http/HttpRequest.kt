package com.example.fragment.library.base.http

import android.text.TextUtils
import java.io.File
import java.util.LinkedHashSet
import java.util.regex.Pattern

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
    companion object {
        private const val PARAM = "[a-zA-Z][a-zA-Z0-9_-]*"
        private var PARAM_URL_REGEX = Pattern.compile("\\{($PARAM)\\}")
    }

    private var baseUrl: String = ""

    fun setBaseUrl(baseUrl: String): HttpRequest {
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
        if (query.isNotEmpty()) {
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
        }
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