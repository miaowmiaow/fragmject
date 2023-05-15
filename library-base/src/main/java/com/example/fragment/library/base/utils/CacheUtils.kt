package com.example.fragment.library.base.utils

import android.content.Context
import java.io.File

object CacheUtils {

    fun getDirPath(context: Context, name: String): String {
        return if (FileUtil.isSDCardAlive()) {
            File(context.externalCacheDir, name).apply { mkdirs() }.absolutePath
        } else {
            File(context.cacheDir, name).apply { mkdirs() }.absolutePath
        }
    }

    fun getTotalSize(context: Context): String {
        var cacheSize = FileUtil.getSize(context.cacheDir)
        if (FileUtil.isSDCardAlive()) {
            context.externalCacheDir?.apply {
                cacheSize += FileUtil.getSize(this)
            }
        }
        return FileUtil.formatSize(cacheSize.toDouble())
    }

    fun clearAllCache(context: Context) {
        FileUtil.delete(context.cacheDir)
        if (FileUtil.isSDCardAlive()) {
            FileUtil.delete(context.externalCacheDir)
        }
    }

}