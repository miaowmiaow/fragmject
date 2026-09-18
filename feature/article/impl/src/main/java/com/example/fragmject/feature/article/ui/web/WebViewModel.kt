package com.example.fragmject.feature.article.ui.web

import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.repository.VideoDownloadRepository
import com.example.fragmject.core.model.History
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Web 页的 ViewModel：承载书签状态、浏览历史写入与媒体交互编排。
 *
 * 图片保存与视频下载的 repository 调用收拢到这里，
 * Composable 只负责对话框状态与 Toast 反馈，不再直接触碰领域端口。
 */
@HiltViewModel
class WebViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
    private val mediaRepository: MediaRepository,
    private val videoDownloadRepository: VideoDownloadRepository,
) : ViewModel() {

    private val _bookmark = MutableStateFlow<History?>(null)
    val bookmark: StateFlow<History?> = _bookmark.asStateFlow()

    private var currentUrl: String = ""

    fun init(url: String) {
        if (currentUrl == url) return
        currentUrl = url
        viewModelScope.launch {
            historyRepo.observeBookmarks().collect { bookmarks ->
                _bookmark.value = bookmarks.firstOrNull { it.url == url }
            }
        }
    }

    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            val current = _bookmark.value
            if (current != null) historyRepo.deleteHistory(current)
            else historyRepo.setBookmark(title, url)
        }
    }

    fun setBrowseHistory(title: String, url: String) {
        viewModelScope.launch { historyRepo.setBrowseHistory(title, url) }
    }

    /** 保存图片：URL 走下载保存，base64 走解码保存，结果通过 [onResult] 回传。 */
    fun saveImage(extra: String?, onResult: (Boolean) -> Unit) {
        if (extra == null) {
            onResult(false)
            return
        }
        if (URLUtil.isValidUrl(extra)) {
            mediaRepository.saveImageToAlbum(extra) { onResult(it.success) }
        } else {
            mediaRepository.saveBase64ImageToAlbum(extra) { onResult(it.success) }
        }
    }

    /** 注册视频下载任务，标题为空时回退为 URL 末段文件名。 */
    fun registerVideo(title: String?, url: String) {
        videoDownloadRepository.register(
            title = title ?: url.substringAfterLast("/").substringBefore("?"),
            url = url,
        )
    }
}
