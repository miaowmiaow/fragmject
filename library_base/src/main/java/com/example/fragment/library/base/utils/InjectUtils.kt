package com.example.fragment.library.base.utils

import android.content.Context

object InjectUtils {

    fun injectDarkModeJs(context: Context): String? {
        return try {
            context.resources.assets.open("js/darkmode.js").use {
                val buffer = ByteArray(it.available())
                it.read(buffer)
                String(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun newDarkModeJs(): String {
        return """
                    const darkmode = new Darkmode();
                    if(!darkmode.isActivated()){darkmode.toggle();}
        """.trimIndent()
    }

    fun injectVConsoleJs(context: Context): String? {
        return try {
            context.resources.assets.open("js/vconsole.min.js").use {
                val buffer = ByteArray(it.available())
                it.read(buffer)
                String(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun newVConsoleJs(): String {
        return """
                    var vConsole = new VConsole();
        """.trimIndent()
    }
}