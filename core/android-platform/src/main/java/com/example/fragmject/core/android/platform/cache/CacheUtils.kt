package com.example.fragmject.core.android.platform.cache


import android.content.Context
import com.example.fragmject.core.android.platform.file.FileUtils
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

    /**
     * 清理缓存目录，保留 [protectedNames] 中列出的子目录。
     *
     * 这些目录由进程内单例长期持有（Coil DiskCache、OkHttp 磁盘缓存、ExoPlayer SimpleCache），
     * 整体删除会破坏其索引/锁（media3 还要求同目录单一实例），因此必须排除。
     *
     * 注：本函数的清理范围与 [getTotalSize] 的统计范围由此出现差异（统计含常驻目录，
     * 清理不含）。若要求二者一致，需改造统计口径；当前取舍是「宁可少删，不可删坏」。
     */
    suspend fun clearAllCache(
        context: Context,
        protectedNames: Set<String> = emptySet(),
    ) {
        withContext(Dispatchers.IO) {
            deleteChildren(context.cacheDir, protectedNames)
            if (FileUtils.isSDCardAlive()) {
                deleteChildren(context.externalCacheDir, protectedNames)
            }
        }
    }

    /** 删除目录下除 [protectedNames] 外的全部内容（目录本身保留）。 */
    private fun deleteChildren(dir: File?, protectedNames: Set<String>) {
        val target = dir ?: return
        target.listFiles()?.forEach { child ->
            if (child.name !in protectedNames) {
                FileUtils.delete(child)
            }
        }
    }

}
