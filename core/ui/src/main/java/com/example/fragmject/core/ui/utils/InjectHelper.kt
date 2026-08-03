package com.example.fragmject.core.ui.utils

import android.content.Context

/**
 * JS 脚本懒加载缓存：避免每次 WebView 加载新页面时都从 assets 中打开/读取/创建 String。
 * 这两个脚本在应用生命周期内不变，启动后只读取一次，后续调用零 IO 开销。
 */
object JsInjectCache {
    @Volatile
    private var vConsoleJs: String? = null

    @Volatile
    private var quickVideoJs: String? = null

    fun vConsoleJs(context: Context): String {
        return vConsoleJs ?: synchronized(this) {
            vConsoleJs ?: readAsset(context, "js/vconsole.min.js")?.let { js ->
                """
                    $js
                    var vConsole = new VConsole();
                """.trimIndent()
            }.also { vConsoleJs = it } ?: ""
        }
    }

    fun quickVideoJs(context: Context): String {
        return quickVideoJs ?: synchronized(this) {
            quickVideoJs ?: readAsset(context, "js/quick-video.js")?.trimIndent()
                .also { quickVideoJs = it } ?: ""
        }
    }

    private fun readAsset(context: Context, path: String): String? {
        return try {
            val bytes = context.resources.assets.open(path).use { input ->
                ByteArray(input.available()).also { input.read(it) }
            }
            String(bytes)
        } catch (e: Exception) {
            null
        }
    }
}

/** @deprecated 改用 [JsInjectCache.vConsoleJs]，避免每次打开 assets 文件。 */
@Deprecated("Use JsInjectCache.vConsoleJs()", ReplaceWith("JsInjectCache.vConsoleJs(this)"))
fun Context.injectVConsoleJs(): String = JsInjectCache.vConsoleJs(this)

/** @deprecated 改用 [JsInjectCache.quickVideoJs]，避免每次打开 assets 文件。 */
@Deprecated("Use JsInjectCache.quickVideoJs()", ReplaceWith("JsInjectCache.quickVideoJs(this)"))
fun Context.injectQuickVideoJs(): String = JsInjectCache.quickVideoJs(this)