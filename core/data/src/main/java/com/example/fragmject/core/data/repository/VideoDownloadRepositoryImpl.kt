package com.example.fragmject.core.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.fragmject.core.common.utils.CacheUtils
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.repository.VideoDownloadRepository
import com.example.fragmject.core.domain.repository.VideoDownloadStatus
import com.example.fragmject.core.domain.repository.VideoDownloadTask
import com.example.fragmject.core.domain.result.DownloadResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 视频下载任务管理 data 实现。
 *
 * 自包含「下载编排 + 任务状态维护 + 断点续传路径持久化」：
 * - [register] 提交任务后，内部自动完成 m3u8/mp4 下载、进度上报、状态更新；
 * - 任务列表持久化到内部存储 JSON 文件，构造时自动恢复并续传未完成任务。
 */
@Singleton
class VideoDownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
    private val mediaRepository: MediaRepository,
) : VideoDownloadRepository {

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _tasks = MutableStateFlow<List<VideoDownloadTask>>(emptyList())
    override val tasks: StateFlow<List<VideoDownloadTask>> = _tasks.asStateFlow()

    private val persistFile = File(context.filesDir, "video_download_tasks.json")
    private val lastSavedProgress = mutableMapOf<String, Float>()

    init {
        loadFromDisk()
    }

    override fun register(title: String, url: String): String {
        // 去重：同 URL 已有 Downloading/Pending 任务则复用，避免重复下载
        val existing = _tasks.value.find {
            it.url == url && (it.status == VideoDownloadStatus.Downloading || it.status == VideoDownloadStatus.Pending)
        }
        if (existing != null) {
            return existing.id
        }
        val id = UUID.randomUUID().toString()
        val task = VideoDownloadTask(id, title, url, VideoDownloadStatus.Downloading, progress = 0f)
        _tasks.update { it + task }
        downloadScope.launch { performDownload(task) }
        saveToDisk()
        return id
    }

    override fun retry(id: String) {
        _tasks.update { tasks ->
            tasks.map {
                if (it.id == id) it.copy(
                    status = VideoDownloadStatus.Downloading,
                    progress = 0f
                ) else it
            }
        }
        val task = _tasks.value.find { it.id == id } ?: return
        downloadScope.launch { performDownload(task) }
        saveToDisk()
    }

    override fun remove(id: String) {
        _tasks.update { tasks -> tasks.filter { it.id != id } }
        saveToDisk()
    }

    override fun clear() {
        _tasks.update { emptyList() }
        saveToDisk()
    }

    // ── 状态更新（内部） ──

    private fun updateProgress(id: String, progress: Float) {
        _tasks.update { tasks ->
            tasks.map { if (it.id == id) it.copy(progress = progress.coerceIn(0f, 1f)) else it }
        }
        // 重大进度节点才存盘（>20% 或 >0.5 跳跃），避免高频 IO
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > 0.2f || (clamped - (lastSavedProgress[id] ?: -1f)) > 0.5f) {
            lastSavedProgress[id] = clamped
            saveToDisk()
        }
    }

    private fun markComplete(id: String, outputPath: String) {
        _tasks.update { tasks ->
            tasks.map {
                if (it.id == id) it.copy(
                    status = VideoDownloadStatus.Complete,
                    progress = 1f,
                    outputPath = outputPath
                ) else it
            }
        }
        saveToDisk()
    }

    private fun markFailed(id: String) {
        _tasks.update { tasks ->
            tasks.map { if (it.id == id) it.copy(status = VideoDownloadStatus.Failed) else it }
        }
        saveToDisk()
    }

    private fun updateM3u8Paths(
        taskId: String,
        saveDir: String,
        playlistPath: String,
        segTmpDir: String
    ) {
        _tasks.update { tasks ->
            tasks.map {
                if (it.id == taskId) it.copy(
                    saveDir = saveDir,
                    playlistPath = playlistPath,
                    segTmpDir = segTmpDir
                ) else it
            }
        }
    }

    private fun cleanupM3u8Paths(taskId: String) {
        _tasks.update { tasks ->
            tasks.map {
                if (it.id == taskId) it.copy(
                    saveDir = null,
                    playlistPath = null,
                    segTmpDir = null
                ) else it
            }
        }
    }

    // ── 下载编排 ──

    /**
     * 下载视频到 Movies 目录，完成后通过 MediaRepository 通知系统扫描。
     *
     * 策略：
     * 1. 先从 URL path 末段提取合法扩展名（仅 2-5 位字母数字）；
     * 2. 下载到临时文件，检测前 20 字节是否含 #EXTM3U（m3u8 特征码）；
     * 3. 是 m3u8 → 删除临时文件，委托 M3u8Downloader 重新下载+合并；
     * 4. 不是 m3u8 → 重命名为正式文件名。
     */
    private suspend fun performDownload(task: VideoDownloadTask): Boolean {
        val taskId = task.id
        val videoUrl = task.url
        val title = task.title
        try {
            val baseDir = CacheUtils.getDirPath(context, Environment.DIRECTORY_MOVIES)
            val safeTitle =
                title.take(40).replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "video" }
            val saveDir = File(baseDir, safeTitle).also { it.mkdirs() }.absolutePath

            val pathExt = videoUrl
                .substringBefore("?")
                .substringAfterLast("/")
                .substringAfterLast(".", "")
                .lowercase()
            val ext =
                if (pathExt.length in 2..5 && pathExt.all { it in 'a'..'z' || it in '0'..'9' })
                    pathExt else ""

            // ── m3u8 断点续传检测 ──
            val resumePlaylist = task.playlistPath?.let { File(it) }?.takeIf { it.exists() }
            val resumeSegDir = task.segTmpDir?.let { File(it) }?.takeIf { it.isDirectory }

            if (ext == "m3u8" || resumePlaylist != null) {
                val merged: File? = withContext(Dispatchers.IO) {
                    if (resumePlaylist != null && resumeSegDir != null) {
                        // 断点续传：跳过已下载的分片
                        M3u8Downloader.downloadResumable(
                            videoUrl,
                            saveDir,
                            resumePlaylist,
                            resumeSegDir,
                            downloadRepository
                        ) { p ->
                            updateProgress(taskId, p * 0.95f)
                        }
                    } else {
                        // 首次下载 m3u8
                        val mergedFile =
                            M3u8Downloader.download(videoUrl, saveDir, downloadRepository) { p ->
                                updateProgress(taskId, 0.05f + p * 0.9f)
                            }
                        // 下载过程中保存 playlist 和 seg 目录路径到 Task，供断点续传
                        if (mergedFile != null) {
                            saveM3u8ResumePaths(taskId, saveDir)
                        }
                        mergedFile
                    }
                }
                if (merged != null) {
                    cleanupM3u8Paths(taskId)
                    mediaRepository.notifyMediaAdded(merged.absolutePath) { }
                    markComplete(taskId, merged.absolutePath)
                    return true
                }
                markFailed(taskId)
                return false
            }

            updateProgress(taskId, 0.1f)
            val tmpName = "video_tmp_${System.currentTimeMillis()}"

            // 全量下载（兜底）：每次尝试带 2 分钟超时，最多重试 3 次
            val mp4RetryDelays = longArrayOf(500, 1000, 2000)
            val mp4AttemptTimeout = 120L
            var result: DownloadResult? = null
            for (attempt in 0..mp4RetryDelays.size) {
                updateProgress(taskId, 0.10f + 0.01f * attempt)
                val tmpFileAttempt = File(saveDir, tmpName + "_" + attempt)
                result = try {
                    withTimeoutOrNull(mp4AttemptTimeout.seconds) {
                        withContext(Dispatchers.IO) {
                            downloadRepository.download(videoUrl, saveDir, tmpFileAttempt.name)
                        }
                    }
                } catch (_: Exception) {
                    null
                }
                if (result != null && result.success) {
                    tmpFileAttempt.renameTo(File(saveDir, tmpName))
                    break
                }
                if (tmpFileAttempt.exists()) tmpFileAttempt.delete()
                if (attempt < mp4RetryDelays.size) {
                    Log.w(
                        TAG,
                        "MP4 download attempt ${attempt + 1} failed, retrying in ${mp4RetryDelays[attempt]}ms: $videoUrl"
                    )
                    delay(mp4RetryDelays[attempt].milliseconds)
                } else {
                    Log.e(
                        TAG,
                        "MP4 download failed after ${mp4RetryDelays.size + 1} attempts: $videoUrl"
                    )
                }
            }
            if (result == null || !result.success) {
                markFailed(taskId); return false
            }

            val tmpFile = File(saveDir, tmpName)
            if (!tmpFile.exists() || tmpFile.length() == 0L) {
                tmpFile.delete(); markFailed(taskId); return false
            }
            updateProgress(taskId, 0.8f)

            val isM3u8Content = tmpFile.length() < 2 * 1024 * 1024 && try {
                val header = ByteArray(20).also { tmpFile.inputStream().use { s -> s.read(it) } }
                String(header, Charsets.UTF_8).contains("#EXTM3U")
            } catch (_: Exception) {
                false
            }

            if (isM3u8Content) {
                tmpFile.delete()
                val merged = M3u8Downloader.download(videoUrl, saveDir, downloadRepository) { p ->
                    updateProgress(taskId, 0.8f + p * 0.15f)
                }
                if (merged != null) {
                    cleanupM3u8Paths(taskId)
                    mediaRepository.notifyMediaAdded(merged.absolutePath) { }
                    markComplete(taskId, merged.absolutePath)
                    return true
                }
                markFailed(taskId)
                return false
            }

            val finalExt = ext.ifBlank { "mp4" }
            val outputFile = File(saveDir, "video_${System.currentTimeMillis()}.$finalExt")
            tmpFile.renameTo(outputFile)

            mediaRepository.notifyMediaAdded(outputFile.absolutePath) { }
            markComplete(taskId, outputFile.absolutePath)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "downloadVideo failed: $videoUrl", e)
            markFailed(taskId)
            return false
        }
    }

    /** 扫描 saveDir 中最新生成的 playlist 和 seg_tmp 目录，保存到 Task 供断点续传。 */
    private fun saveM3u8ResumePaths(taskId: String, saveDir: String) {
        try {
            val dir = File(saveDir)
            val playlist = dir.listFiles()?.filter {
                it.name.startsWith("playlist_") && it.name.endsWith(".m3u8")
            }?.maxByOrNull { it.lastModified() }
            val segDir = dir.listFiles()?.filter {
                it.isDirectory && it.name.startsWith("seg_tmp_")
            }?.maxByOrNull { it.lastModified() }
            if (playlist != null && segDir != null) {
                updateM3u8Paths(
                    taskId, saveDir,
                    playlist.absolutePath, segDir.absolutePath
                )
            }
        } catch (_: Exception) {
        }
    }

    // ── 持久化 ──

    private fun saveToDisk() {
        val json = buildTasksJson()
        downloadScope.launch(Dispatchers.IO) {
            try {
                persistFile.writeText(json)
            } catch (_: Exception) {
            }
        }
    }

    private fun loadFromDisk() {
        val json = try {
            persistFile.takeIf { it.exists() && it.length() > 0 }?.readText()
        } catch (_: Exception) {
            null
        }
        if (json.isNullOrBlank()) return

        val all = parseTasksJson(json)
        val completeOrFailed = all.filter { task ->
            when (task.status) {
                VideoDownloadStatus.Complete -> task.outputPath?.let { File(it).exists() } == true
                VideoDownloadStatus.Failed -> true
                else -> false
            }
        }.map { task ->
            task.copy(progress = if (task.status == VideoDownloadStatus.Complete) 1f else 0f)
        }

        // 恢复未完成任务：URL 去重（保留第一个），保留上次存盘的进度
        val seenUrls = mutableSetOf<String>()
        val downloading = all
            .filter { it.status == VideoDownloadStatus.Downloading || it.status == VideoDownloadStatus.Pending }
            .filter { seenUrls.add(it.url) }
            .map { it.copy(status = VideoDownloadStatus.Downloading) }

        _tasks.value = completeOrFailed + downloading

        // 自动恢复未完成的下载
        downloading.forEach { task ->
            downloadScope.launch { performDownload(task) }
        }
    }

    private fun buildTasksJson(): String {
        val arr = JSONArray()
        _tasks.value.forEach { task ->
            arr.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("url", task.url)
                put("status", task.status.name)
                put("progress", task.progress.toDouble())
                task.outputPath?.let { put("outputPath", it) }
                task.saveDir?.let { put("saveDir", it) }
                task.playlistPath?.let { put("playlistPath", it) }
                task.segTmpDir?.let { put("segTmpDir", it) }
            })
        }
        return arr.toString()
    }

    private fun parseTasksJson(json: String): List<VideoDownloadTask> {
        val result = mutableListOf<VideoDownloadTask>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val status = try {
                    VideoDownloadStatus.valueOf(obj.getString("status"))
                } catch (_: Exception) {
                    VideoDownloadStatus.Failed
                }
                val progress = obj.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f)
                val outputPath = obj.optString("outputPath", "").ifBlank { null }
                val saveDir = obj.optString("saveDir", "").ifBlank { null }
                val playlistPath = obj.optString("playlistPath", "").ifBlank { null }
                val segTmpDir = obj.optString("segTmpDir", "").ifBlank { null }
                result.add(
                    VideoDownloadTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        status = status,
                        progress = progress,
                        outputPath = outputPath,
                        saveDir = saveDir,
                        playlistPath = playlistPath,
                        segTmpDir = segTmpDir,
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    private companion object {
        const val TAG = "VideoDownloadRepository"
    }
}
