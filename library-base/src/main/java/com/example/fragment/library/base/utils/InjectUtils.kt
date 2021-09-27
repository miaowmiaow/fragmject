package com.example.fragment.library.base.utils

import android.content.Context
import com.example.fragment.library.base.utils.UIModeUtils.isNightMode

object InjectUtils {

    fun Context.injectDarkModeJs(): String? {
        return try {
            resources.assets.open("js/darkmode.js").use {
                val buffer = ByteArray(it.available())
                it.read(buffer)
                String(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Context.newDarkModeJs(): String {
        val forceDarkMode = isNightMode()
        return """
                    const darkmode = new Darkmode();
                    if($forceDarkMode){
                        if(!darkmode.isActivated()){
                            darkmode.toggle();
                        }
                    }else{
                        if(darkmode.isActivated()){
                            darkmode.toggle();
                        }
                    }
                    console.log(darkmode.isActivated())
        """.trimIndent()
    }

    fun Context.injectVConsoleJs(): String? {
        return try {
            resources.assets.open("js/vconsole.min.js").use {
                val buffer = ByteArray(it.available())
                it.read(buffer)
                String(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Context.newVConsoleJs(): String {
        return """
                    var vConsole = new VConsole();
        """.trimIndent()
    }
}