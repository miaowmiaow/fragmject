package com.example.miaow.base.utils

import android.content.Context

fun Context.injectVConsoleJs(): String {
    return try {
        val vconsoleJs = resources.assets.open("js/vconsole.min.js").use {
            val buffer = ByteArray(it.available())
            it.read(buffer)
            String(buffer)
        }
        """ 
            $vconsoleJs
            var vConsole = new VConsole();
        """.trimIndent()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}