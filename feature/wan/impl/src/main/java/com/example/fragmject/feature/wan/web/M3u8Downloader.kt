package com.example.fragmject.feature.wan.web

import android.util.Log
import com.example.fragmject.core.network.http.download
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * m3u8 视频下载器。
 *
 * 流程：
 * 1. 下载并解析 .m3u8 播放列表，提取所有 .ts 分片 URL；
 * 2. 并发下载所有 .ts 分片（Semaphore 限制并发数=4，避免 IO 过载）；
 * 3. 按顺序将 .ts 文件二进制拼接为单个 .ts 文件（可在大部分播放器直接播放）；
 * 4. 删除中间分片文件。
 *
 * 不支持：加密 m3u8（#EXT-X-KEY）—— 需密钥解密，超出本工具范围。
 */
object M3u8Downloader {

    private const val TAG = "M3u8Downloader"
    private const val MAX_CONCURRENT = 4
    private const val DOWNLOAD_TIMEOUT_S = 30L
    private const val SEGMENT_TIMEOUT_S = 20L

    /** m3u8 分片条目。 */
    data class Segment(val index: Int, val url: String)

    /**
     * 下载 m3u8 视频到 [saveDir] 目录。
     *
     * @param m3u8Url  .m3u8 播放列表的完整 URL
     * @param saveDir 保存目录（通常为 CacheUtils.getDirPath(context, Environment.DIRECTORY_MOVIES)）
     * @param onProgress 进度回调：0f~1f
     * @return 合并后的 .ts 文件，失败时返回 null
     */
    suspend fun download(
        m3u8Url: String,
        saveDir: String,
        onProgress: (Float) -> Unit = {},
    ): File? {
        // 1) 下载并解析 m3u8
        val segments = fetchAndParse(m3u8Url, saveDir)
        if (segments.isEmpty()) {
            Log.e(TAG, "No segments parsed from: $m3u8Url")
            return null
        }
        onProgress(0.05f)

        return withTimeoutOrNull(
            (DOWNLOAD_TIMEOUT_S * segments.size.coerceAtLeast(1)).seconds
        ) {
            val segTmpDir = File(saveDir, "seg_tmp_${System.currentTimeMillis()}")
            segTmpDir.mkdirs()
            try {
                // 2) 并发下载分片
                val tsFiles = downloadSegments(segments, segTmpDir.absolutePath) { done, total ->
                    onProgress(0.05f + 0.85f * done / total.coerceAtLeast(1))
                }
                if (tsFiles.size < segments.size) {
                    Log.w(TAG, "Only ${tsFiles.size}/${segments.size} segments downloaded; " +
                            "merged duration may be shorter than expected")
                }
                if (tsFiles.isEmpty()) {
                    Log.e(TAG, "All segment downloads failed for: $m3u8Url")
                    return@withTimeoutOrNull null
                }

                // 3) 流式合并 → .mp4
                val outputName = "video_${System.currentTimeMillis()}.mp4"
                val outputFile = File(saveDir, outputName)
                mergeTsFiles(tsFiles, outputFile)
                onProgress(0.95f)

                outputFile
            } finally {
                // 无论成功/超时都清理 seg 临时目录
                segTmpDir.deleteRecursively()
                onProgress(1f)
            }
        }
    }

    /**
     * 断点续传：基于已保存的播放列表和分片目录恢复下载。
     *
     * @param m3u8Url    m3u8 播放列表 URL（如果 playlist 文件丢失则重新下载）
     * @param saveDir   输出目录
     * @param playlistFile 已保存的 playlist 文件
     * @param segTmpDir   已有的分片临时目录
     * @param onProgress  进度回调 0f..1f
     */
    suspend fun downloadResumable(
        m3u8Url: String,
        saveDir: String,
        playlistFile: File,
        segTmpDir: File,
        onProgress: (Float) -> Unit = {},
    ): File? {
        // 1) 解析已有 playlist（如果文件丢失则重新下载）
        val segments = if (playlistFile.exists()) {
            parsePlaylist(playlistFile.readText(Charsets.UTF_8), m3u8Url)
        } else {
            // playlist 文件丢失，回退到普通下载
            return download(m3u8Url, saveDir, onProgress)
        }
        if (segments.isEmpty()) {
            Log.e(TAG, "No segments parsed for resume: $m3u8Url")
            return null
        }
        onProgress(0.02f)

        return withTimeoutOrNull(
            (DOWNLOAD_TIMEOUT_S * segments.size.coerceAtLeast(1)).seconds
        ) {
            segTmpDir.mkdirs()

            // 2) 统计已下载的分片（文件存在且 > 0 字节），只下载缺失的
            val existingCount = segments.count { seg ->
                File(segTmpDir, "seg_${seg.index}.ts").let { it.exists() && it.length() > 0 }
            }
            if (existingCount > 0) {
                Log.d(TAG, "Resuming: $existingCount/${segments.size} segments already downloaded")
            }
            onProgress(0.05f + 0.02f * existingCount / segments.size.coerceAtLeast(1))

            try {
                // 3) 只下载缺失的分片
                val tsFiles = downloadSegmentsResumable(segments, segTmpDir) { done, total ->
                    onProgress(0.05f + 0.85f * done / total.coerceAtLeast(1))
                }
                if (tsFiles.size < segments.size) {
                    Log.w(TAG, "Only ${tsFiles.size}/${segments.size} segments downloaded; " +
                            "merged duration may be shorter than expected")
                }
                if (tsFiles.isEmpty()) {
                    Log.e(TAG, "All segment downloads failed for resume: $m3u8Url")
                    return@withTimeoutOrNull null
                }

                // 4) 流式合并
                val outputName = "video_${System.currentTimeMillis()}.mp4"
                val outputFile = File(saveDir, outputName)
                mergeTsFiles(tsFiles, outputFile)
                onProgress(0.95f)

                // 5) 清理：删除 playlist 和 seg 临时目录
                playlistFile.delete()
                segTmpDir.deleteRecursively()
                onProgress(1f)

                outputFile
            } finally {
                // 超时/异常时不清理 seg 目录，下次还能续传
                onProgress(1f)
            }
        }
    }

    // ----- 内部实现 -----

    /** 下载 m3u8 文件并解析出 .ts 分片列表。播放列表保存到 saveDir 供排查。 */
    private suspend fun fetchAndParse(m3u8Url: String, saveDir: String): List<Segment> {
        val playlistFile = File(saveDir, "playlist_${System.currentTimeMillis()}.m3u8")

        return try {
            val result = withTimeoutOrNull(DOWNLOAD_TIMEOUT_S.seconds) {
                download(saveDir, playlistFile.name) { setUrl(m3u8Url) }
            }
            if (result == null || result.errorCode != "0" || !playlistFile.exists()) {
                playlistFile.delete()
                return emptyList()
            }

            val content = playlistFile.readText(Charsets.UTF_8)
            // 保留 playlist 文件，方便事后核对时长、排查丢片
            parsePlaylist(content, m3u8Url)
        } catch (e: Exception) {
            Log.e(TAG, "fetchAndParse failed: $m3u8Url", e)
            playlistFile.delete()
            emptyList()
        }
    }

    /** 解析 m3u8 内容，提取 .ts URL。 */
    private fun parsePlaylist(content: String, baseUrl: String): List<Segment> {
        val lines = content.lines()
        val segments = mutableListOf<Segment>()
        var index = 0
        var nextUrl: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXT-X-KEY") -> {
                    Log.w(TAG, "Encrypted m3u8 detected (EXT-X-KEY), download may fail")
                }
                trimmed.startsWith("#EXTINF") -> {
                    // 下一个非注释、非空行即为 .ts URL
                }
                !trimmed.startsWith("#") && trimmed.isNotBlank() -> {
                    nextUrl = trimmed
                }
            }
            if (nextUrl != null) {
                val fullUrl = if (nextUrl.startsWith("http")) nextUrl
                else baseUrl.substringBeforeLast("/") + "/" + nextUrl.trimStart('/')
                segments.add(Segment(index++, fullUrl))
                nextUrl = null
            }
        }
        return segments
    }

    /** 并发下载所有 .ts 分片。 */
    private suspend fun downloadSegments(
        segments: List<Segment>,
        saveDir: String,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<File> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT)
        var completed = 0

        segments.map { seg ->
            async {
                semaphore.withPermit {
                    val outFile = File(saveDir, "seg_${seg.index}.ts")
                    val success = downloadSingleSegment(seg.url, outFile)
                    synchronized(this@coroutineScope) {
                        completed++
                        onProgress(completed, segments.size)
                    }
                    if (success) outFile else null
                }
            }
        }.awaitAll().filterNotNull().sortedBy { file ->
            // seg_{index}.ts → 按数字 index 排序，而非字符串序（否则 seg_10 < seg_2）
            file.name.removePrefix("seg_").removeSuffix(".ts").toIntOrNull() ?: 0
        }
    }

    /** 断点续传下载分片：跳过 segTmpDir 中已存在且 > 0 字节的文件。 */
    private suspend fun downloadSegmentsResumable(
        segments: List<Segment>,
        segTmpDir: File,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<File> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT)
        var completed = 0
        val total = segments.size

        segments.map { seg ->
            async {
                semaphore.withPermit {
                    val outFile = File(segTmpDir, "seg_${seg.index}.ts")
                    val success = if (outFile.exists() && outFile.length() > 0) {
                        // 分片已存在，跳过下载
                        synchronized(this@coroutineScope) { completed++; onProgress(completed, total) }
                        true
                    } else {
                        val ok = downloadSingleSegment(seg.url, outFile)
                        synchronized(this@coroutineScope) { completed++; onProgress(completed, total) }
                        ok
                    }
                    if (success) outFile else null
                }
            }
        }.awaitAll().filterNotNull().sortedBy { file ->
            file.name.removePrefix("seg_").removeSuffix(".ts").toIntOrNull() ?: 0
        }
    }

    /** 下载单个 .ts 分片，最多重试 3 次。 */
    private suspend fun downloadSingleSegment(url: String, outFile: File): Boolean {
        val dir = outFile.parent ?: return false
        val delaysMs = longArrayOf(500, 1000, 2000)
        for (attempt in 0..delaysMs.size) {
            val ok = try {
                val result = withTimeoutOrNull(SEGMENT_TIMEOUT_S.seconds) {
                    download(dir, outFile.name) { setUrl(url) }
                }
                result != null && result.errorCode == "0" && outFile.exists() && outFile.length() > 0
            } catch (_: Exception) {
                if (outFile.exists()) outFile.delete()
                false
            }
            if (ok) return true
            if (attempt < delaysMs.size) {
                if (outFile.exists()) outFile.delete()
                kotlinx.coroutines.delay(delaysMs[attempt].milliseconds)
            } else {
                Log.w(TAG, "Segment failed after ${delaysMs.size + 1} attempts: $url")
            }
        }
        return false
    }

    /** 流式合并 .ts 文件，避免大文件 OOM。 */
    private fun mergeTsFiles(tsFiles: List<File>, output: File) {
        FileOutputStream(output).use { fos ->
            val buffer = ByteArray(64 * 1024) // 64KB
            for (tsFile in tsFiles) {
                FileInputStream(tsFile).use { fis ->
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } > 0) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }
}
