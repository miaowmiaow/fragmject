package com.example.fragment.library.base.utils

import android.content.Context
import java.io.File

object CacheUtils {

    fun getCacheDirPath(context: Context, name: String): String {
        return File(context.cacheDir, name).apply { mkdirs() }.absolutePath
    }

    fun getTotalCacheSize(context: Context): String {
        var cacheSize = FileUtils.getSize(context.cacheDir)
        if (FileUtils.isSDCardAlive()) {
            context.externalCacheDir?.apply {
                cacheSize += FileUtils.getSize(this)
            }
        }
        return FileUtils.formatSize(cacheSize.toDouble())
    }

    fun clearAllCache(context: Context) {
        FileUtils.delete(context.cacheDir)
        if (FileUtils.isSDCardAlive()) {
            FileUtils.delete(context.externalCacheDir)
        }
    }
}