package com.example.miaow.base.utils

import android.util.Log
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
                Log.e(this.javaClass.name, e.message.toString())
                MainThreadExecutor.get().execute {
                    onResponse.invoke(null)
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

}