package com.example.fragmject.feature.wan.web

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 视频下载管理器：单例维护所有下载任务的状态和进度。
 *
 * - UI 层通过 [tasks] StateFlow 观察，每项下载进度 0f-1f 实时更新。
 * - 任务列表持久化到内部存储 JSON 文件，App 重启后自动恢复。
 */
object VideoDownloadManager {

    /** 下载任务的不可变快照。 */
    data class Task(
        val id: String,
        val title: String,
        val url: String,
        val status: Status,
        val progress: Float = 0f,
        val outputPath: String? = null,
        /** m3u8 断点续传：下载目录（Movies/子目录） */
        val saveDir: String? = null,
        /** m3u8 断点续传：已保存的播放列表路径 */
        val playlistPath: String? = null,
        /** m3u8 断点续传：seg 分片临时目录路径 */
        val segTmpDir: String? = null,
    )

    enum class Status { Pending, Downloading, Complete, Failed }

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private var persistFile: File? = null
    private var restartBlock: (suspend (Task) -> Unit)? = null
    private val lastSavedProgress = mutableMapOf<String, Float>()

    /** 初始化：从磁盘恢复历史任务，并自动重启未完成的下载。应在主界面首次组合时调用。 */
    fun init(context: Context, restartBlock: suspend (Task) -> Unit) {
        this.restartBlock = restartBlock
        persistFile = File(context.filesDir, "video_download_tasks.json")
        loadFromDisk()
    }

    fun register(title: String, url: String, downloadBlock: suspend (taskId: String) -> Unit): String {
        // 去重：同 URL 已有 Downloading/Pending 任务则复用，避免重复下载
        val existing = _tasks.value.find {
            it.url == url && (it.status == Status.Downloading || it.status == Status.Pending)
        }
        if (existing != null) {
            return existing.id
        }
        val id = UUID.randomUUID().toString()
        _tasks.update { it + Task(id, title, url, Status.Downloading, progress = 0f) }
        downloadScope.launch { downloadBlock(id) }
        saveToDisk()
        return id
    }

    fun onProgress(id: String, progress: Float) {
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

    fun onComplete(id: String, outputPath: String) {
        _tasks.update { tasks ->
            tasks.map { if (it.id == id) it.copy(status = Status.Complete, progress = 1f, outputPath = outputPath) else it }
        }
        saveToDisk()
    }

    fun onFailed(id: String) {
        _tasks.update { tasks ->
            tasks.map { if (it.id == id) it.copy(status = Status.Failed) else it }
        }
        saveToDisk()
    }

    /** m3u8 下载过程中保存断点续传路径，供 App 被杀后恢复。 */
    fun onM3u8Progress(taskId: String, saveDir: String, playlistPath: String, segTmpDir: String) {
        _tasks.update { tasks ->
            tasks.map { if (it.id == taskId) it.copy(saveDir = saveDir, playlistPath = playlistPath, segTmpDir = segTmpDir) else it }
        }
    }

    /** m3u8 下载完成后清除断点续传路径。 */
    fun onM3u8Cleanup(taskId: String) {
        _tasks.update { tasks ->
            tasks.map { if (it.id == taskId) it.copy(saveDir = null, playlistPath = null, segTmpDir = null) else it }
        }
    }

    /** 重试失败的任务。通过保存的 restartBlock 重新触发下载。 */
    fun retry(id: String) {
        val block = restartBlock ?: return
        _tasks.update { tasks ->
            tasks.map { if (it.id == id) it.copy(status = Status.Downloading, progress = 0f) else it }
        }
        val task = _tasks.value.find { it.id == id } ?: return
        downloadScope.launch { block(task) }
        saveToDisk()
    }

    fun remove(id: String) {
        _tasks.update { tasks -> tasks.filter { it.id != id } }
        saveToDisk()
    }

    fun clear() {
        _tasks.update { emptyList() }
        saveToDisk()
    }

    // ── 持久化 ──

    private fun saveToDisk() {
        val file = persistFile ?: return
        val json = buildTasksJson()
        downloadScope.launch(Dispatchers.IO) {
            try { file.writeText(json) } catch (_: Exception) {}
        }
    }

    private fun loadFromDisk() {
        val file = persistFile ?: return
        val json = try { file.takeIf { it.exists() && it.length() > 0 }?.readText() } catch (_: Exception) { null }
        if (json.isNullOrBlank()) return

        val all = parseTasksJson(json)
        val completeOrFailed = all.filter { task ->
            when (task.status) {
                Status.Complete -> task.outputPath != null && File(task.outputPath).exists()
                Status.Failed -> true
                else -> false
            }
        }.map { task ->
            task.copy(progress = if (task.status == Status.Complete) 1f else 0f)
        }

        // 恢复未完成任务：URL 去重（保留第一个），保留上次存盘的进度
        val seenUrls = mutableSetOf<String>()
        val downloading = all
            .filter { it.status == Status.Downloading || it.status == Status.Pending }
            .filter { seenUrls.add(it.url) }  // 同 URL 只保留第一个
            .map { it.copy(status = Status.Downloading) }

        _tasks.value = completeOrFailed + downloading

        // 自动恢复未完成的下载
        val block = restartBlock ?: return
        downloading.forEach { task ->
            downloadScope.launch { block(task) }
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

    private fun parseTasksJson(json: String): List<Task> {
        val result = mutableListOf<Task>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val status = try {
                    Status.valueOf(obj.getString("status"))
                } catch (_: Exception) {
                    Status.Failed
                }
                val progress = obj.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f)
                val outputPath = obj.optString("outputPath", "").ifBlank { null }
                val saveDir = obj.optString("saveDir", "").ifBlank { null }
                val playlistPath = obj.optString("playlistPath", "").ifBlank { null }
                val segTmpDir = obj.optString("segTmpDir", "").ifBlank { null }
                result.add(Task(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    status = status,
                    progress = progress,
                    outputPath = outputPath,
                    saveDir = saveDir,
                    playlistPath = playlistPath,
                    segTmpDir = segTmpDir,
                ))
            }
        } catch (_: Exception) { }
        return result
    }
}
