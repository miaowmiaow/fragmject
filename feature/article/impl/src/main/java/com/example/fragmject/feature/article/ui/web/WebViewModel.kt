package com.example.fragmject.feature.article.ui.web

import com.example.fragmject.core.webview.WebViewCommons
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.model.History
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Web 页的 ViewModel：承载书签状态、浏览历史写入与媒体交互编排。
 *
 * 图片保存的 repository 调用收拢到这里，
 * Composable 只负责对话框状态与 Toast 反馈，不再直接触碰领域端口。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WebViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _bookmark = MutableStateFlow<History?>(null)
    val bookmark: StateFlow<History?> = _bookmark.asStateFlow()

    private val currentUrl = MutableStateFlow("")

    init {
        viewModelScope.launch {
            currentUrl
                .filter { it.isNotBlank() }
                .flatMapLatest { url ->
                    historyRepo.observeBookmarks().map { bookmarks ->
                        bookmarks.firstOrNull { it.url == url }
                    }
                }
                .collect { _bookmark.value = it }
        }
    }

    fun init(url: String) {
        if (currentUrl.value == url) return
        currentUrl.value = url
    }

    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            val current = _bookmark.value
            if (current != null) historyRepo.deleteHistory(current)
            else historyRepo.setBookmark(title, url)
        }
    }

    fun recordBrowseVisit(title: String, url: String) {
        viewModelScope.launch { historyRepo.recordBrowseVisit(title, url) }
    }

    /** 保存图片：URL 走下载保存，base64 走解码保存，结果通过 [onResult] 回传。 */
    fun saveImage(extra: String?, onResult: (Boolean) -> Unit) {
        if (extra == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = if (WebViewCommons.isHttpUrl(extra)) {
                mediaRepository.saveImageToAlbum(extra)
            } else {
                mediaRepository.saveBase64ImageToAlbum(extra)
            }
            onResult(result.success)
        }
    }
}