package com.example.fragment.library.base.http

import android.text.TextUtils
import java.io.File
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap
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
    companion object {
        private const val PARAM = "[a-zA-Z][a-zA-Z0-9_-]*"
        private var PARAM_URL_REGEX = Pattern.compile("\\{($PARAM)\\}")
    }

    fun setUrl(url: String): HttpRequest {
        this.url = url
        return this
    }

    fun getUrl(baseUrl: String? = null): String {
        val m = PARAM_URL_REGEX.matcher(url)
        val patterns: MutableSet<String> = LinkedHashSet()
        while (m.find()) {
            m.group(1)?.let {
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

}