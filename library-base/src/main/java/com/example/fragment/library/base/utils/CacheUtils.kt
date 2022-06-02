package com.example.fragment.library.base.utils

import android.content.Context
import java.io.File

object CacheUtils {

    fun getDirPath(context: Context, name: String): String {
        return if (FileUtils.isSDCardAlive()) {
            File(context.externalCacheDir, name).apply { mkdirs() }.absolutePath
        } else {
            File(context.cacheDir, name).apply { mkdirs() }.absolutePath
        }
    }

    fun getTotalSize(context: Context): String {
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