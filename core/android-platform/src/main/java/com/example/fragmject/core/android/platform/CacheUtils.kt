package com.example.fragmject.core.android.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheUtils {

    fun getDirFile(context: Context, name: String): File {
        return if (FileUtils.isSDCardAlive()) {
            File(context.externalCacheDir, name).apply { mkdirs() }
        } else {
            File(context.cacheDir, name).apply { mkdirs() }
        }
    }

    fun getDirPath(context: Context, name: String): String {
        return getDirFile(context, name).absolutePath
    }

    suspend fun getTotalSize(context: Context): String {
        return withContext(Dispatchers.IO) {
            var cacheSize = FileUtils.getSize(context.cacheDir)
            if (FileUtils.isSDCardAlive()) {
                context.externalCacheDir?.apply {
                    cacheSize += FileUtils.getSize(this)
                }
            }
            FileUtils.formatSize(cacheSize.toDouble())
        }
    }

    suspend fun clearAllCache(context: Context) {
        withContext(Dispatchers.IO) {
            FileUtils.delete(context.cacheDir)
            if (FileUtils.isSDCardAlive()) {
                FileUtils.delete(context.externalCacheDir)
            }
        }
    }

}
