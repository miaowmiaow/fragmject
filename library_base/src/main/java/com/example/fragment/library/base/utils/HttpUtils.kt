package com.example.fragment.library.base.utils

object HttpUtils {

    fun formatPath(url: String, map: Map<String, String>): String {
        val path = StringBuffer(url)
        if (!path.contains("?")){
            path.append("?")
        }
        for ((k, v) in map) {
            path.append("&").append(k).append("=").append(v)
        }
        return path.toString()
    }

}