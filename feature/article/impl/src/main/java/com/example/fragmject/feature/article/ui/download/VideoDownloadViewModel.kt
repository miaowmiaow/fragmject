package com.example.fragmject.feature.article.ui.download

import androidx.lifecycle.ViewModel
import com.example.fragmject.core.domain.repository.VideoDownloadRepository
import com.example.fragmject.core.domain.repository.VideoDownloadTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 视频下载页 ViewModel：观察下载任务列表，并提供移除/重试操作。
 */
@HiltViewModel
class VideoDownloadViewModel @Inject constructor(
    private val videoDownloadRepository: VideoDownloadRepository,
) : ViewModel() {

    val tasks: StateFlow<List<VideoDownloadTask>> = videoDownloadRepository.tasks

    fun remove(id: String) = videoDownloadRepository.remove(id)

    fun retry(id: String) = videoDownloadRepository.retry(id)
}
