package com.example.fragment.library.base.utils

import android.content.Context

object CacheUtils {

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