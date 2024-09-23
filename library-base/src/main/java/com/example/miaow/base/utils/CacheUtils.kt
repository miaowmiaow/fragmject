package com.example.miaow.base.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheUtils {

    fun getDirPath(context: Context, name: String): String {
        return if (FileUtil.isSDCardAlive()) {
            File(context.externalCacheDir, name).apply { mkdirs() }.absolutePath
        } else {
            File(context.cacheDir, name).apply { mkdirs() }.absolutePath
        }
    }

    suspend fun getTotalSize(context: Context): String {
        return withContext(Dispatchers.IO) {
            var cacheSize = FileUtil.getSize(context.cacheDir)
            if (FileUtil.isSDCardAlive()) {
                context.externalCacheDir?.apply {
                    cacheSize += FileUtil.getSize(this)
                }
            }
            FileUtil.formatSize(cacheSize.toDouble())
        }
    }

    fun clearAllCache(context: Context) {
        FileUtil.delete(context.cacheDir)
        if (FileUtil.isSDCardAlive()) {
            FileUtil.delete(context.externalCacheDir)
        }
    }

}