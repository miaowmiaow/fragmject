package com.example.fragmject.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 视频下载任务的不可变快照（纯数据模型）。
 */
data class VideoDownloadTask(
    val id: String,
    val title: String,
    val url: String,
    val status: VideoDownloadStatus,
    val progress: Float = 0f,
    val outputPath: String? = null,
    /** m3u8 断点续传：下载目录（Movies/子目录） */
    val saveDir: String? = null,
    /** m3u8 断点续传：已保存的播放列表路径 */
    val playlistPath: String? = null,
    /** m3u8 断点续传：seg 分片临时目录路径 */
    val segTmpDir: String? = null,
)

enum class VideoDownloadStatus { Pending, Downloading, Complete, Failed }

/**
 * 视频下载任务管理领域端口。
 *
 * 抽象「下载 + 任务状态维护 + 断点续传路径持久化」完整能力：
 * - UI 层通过 [tasks] StateFlow 观察，每项下载进度 0f-1f 实时更新；
 * - [register] 提交新下载任务，内部自动完成下载编排与状态更新；
 * - 任务列表持久化到内部存储 JSON 文件，App 重启后自动恢复并续传。
 */
interface VideoDownloadRepository {
    val tasks: StateFlow<List<VideoDownloadTask>>

    /** 提交新下载任务（同 URL 已有未完成任务则复用）。返回任务 id。 */
    fun register(title: String, url: String): String

    /** 重试失败的任务。 */
    fun retry(id: String)

    /** 移除单个任务。 */
    fun remove(id: String)

    /** 清空所有任务。 */
    fun clear()
}
