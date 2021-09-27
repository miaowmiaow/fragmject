package com.example.fragment.library.base.utils

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HttpUtils {

    fun formatPath(url: String, map: Map<String, String>): String {
        val path = StringBuffer(url)
        if (!path.contains("?")) {
            path.append("?")
        }
        for ((k, v) in map) {
            path.append("&").append(k).append("=").append(v)
        }
        return path.toString()
    }

    fun getContentType(url: String, onResponse: (String?) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                val contentType = connection.getHeaderField("Content-Type")
                MainThreadExecutor.get().execute {
                    onResponse.invoke(contentType)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                MainThreadExecutor.get().execute {
                    onResponse.invoke(null)
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

}