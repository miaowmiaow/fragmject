package com.example.fragmject.feature.wan.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.TitleBar
import java.io.File

/**
 * 视频下载进度页面。
 *
 * - 展示所有下载中的任务及进度；
 * - 点击已完成视频 → 进入 [VideoPlayerScreen] 播放；
 * - 已完成的任务可以清理。
 */
@Composable
fun VideoDownloadScreen(
    onNavigateUp: () -> Unit = {},
    onPlayVideo: (filePath: String) -> Unit = {},
) {
    val tasks by VideoDownloadManager.tasks.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TitleBar(
            title = "视频下载",
            navigationIcon = {
                IconButton(onClick = onNavigateUp, modifier = Modifier.height(45.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无下载任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(tasks, key = { _, t -> t.id }) { _, task ->
                    DownloadTaskItem(
                        task = task,
                        onTap = {
                            if (task.status == VideoDownloadManager.Status.Complete && task.outputPath != null) {
                                onPlayVideo(task.outputPath)
                            }
                        },
                        onRemove = { VideoDownloadManager.remove(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskItem(
    task: VideoDownloadManager.Task,
    onTap: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 标题
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            // 状态图标
            when (task.status) {
                VideoDownloadManager.Status.Pending -> Icon(
                    Icons.Default.Download, contentDescription = "等待中",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)
                )
                VideoDownloadManager.Status.Downloading -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                )
                VideoDownloadManager.Status.Complete -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTap, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.PlayArrow, contentDescription = "播放",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close, contentDescription = "移除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)
                        )
                    }
                }
                VideoDownloadManager.Status.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Close, contentDescription = "失败",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)
                    )
                    IconButton(
                        onClick = { VideoDownloadManager.retry(task.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh, contentDescription = "重试",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close, contentDescription = "移除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        // 进度条 + 百分比（下载中/已完成显示；用真实进度值立即渲染，不做动画延迟）
        if (task.status == VideoDownloadManager.Status.Downloading ||
            task.status == VideoDownloadManager.Status.Complete) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (task.status == VideoDownloadManager.Status.Complete)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${(task.progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // 下载中标签
        if (task.status == VideoDownloadManager.Status.Downloading) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "下载中…",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        // 文件名（已完成时显示）
        if (task.status == VideoDownloadManager.Status.Complete && task.outputPath != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = File(task.outputPath).name,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 失败提示
        if (task.status == VideoDownloadManager.Status.Failed) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "下载失败，请重试",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
