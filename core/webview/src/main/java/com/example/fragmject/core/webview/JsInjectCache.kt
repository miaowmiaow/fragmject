package com.example.fragmject.core.webview

import android.content.Context

/**
 * JS 脚本懒加载缓存：避免每次 WebView 加载新页面时都从 assets 中打开/读取/创建 String。
 * 这些脚本在应用生命周期内不变，启动后只读取一次，后续调用零 IO 开销。
 */
object JsInjectCache {
    @Volatile
    private var vConsoleJs: String? = null

    fun vConsoleJs(context: Context): String {
        return vConsoleJs ?: synchronized(this) {
            vConsoleJs ?: readAsset(context, "js/vconsole.min.js")?.let { js ->
                """
                    $js
                    var VConsole = new VConsole();
                """.trimIndent()
            }.also { vConsoleJs = it } ?: ""
        }
    }

    /**
     * 读取 assets 文本。
     *
     * 用 [readBytes] 而非 `ByteArray(input.available())` + 单次 `read`：
     * available() 不保证等于总长度，单次 read 也不保证填满，历史上存在截断风险。
     * 同时显式指定 UTF-8，避免依赖平台默认字符集。
     */
    private fun readAsset(context: Context, path: String): String? {
        return try {
            context.resources.assets.open(path).use { input ->
                String(input.readBytes(), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            null
        }
    }
}
