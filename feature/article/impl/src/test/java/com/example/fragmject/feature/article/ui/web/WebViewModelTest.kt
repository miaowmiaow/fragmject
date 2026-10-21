package com.example.fragmject.feature.article.ui.web

import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.result.MediaSaveResult
import com.example.fragmject.core.model.History
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [WebViewModel] 测试：书签状态、书签切换与图片保存的路径分流。
 *
 * 图片保存的分流是本类最容易被改坏的地方：http(s) 走下载保存，base64 走解码保存，
 * 空 extra 直接回调失败且不得触碰仓储。
 */
class WebViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - bookmark reflects history matching current url`() = runTest {
        val historyRepo = FakeHistoryRepository(
            bookmarks = listOf(History(id = 1, key = "bookmark", value = "标题", url = "https://a.com")),
        )
        val viewModel = WebViewModel(historyRepo, FakeMediaRepository())

        viewModel.init("https://a.com")

        assertEquals("标题", viewModel.bookmark.value?.value)
    }

    @Test
    fun `init - bookmark stays null for a different url`() = runTest {
        val historyRepo = FakeHistoryRepository(
            bookmarks = listOf(History(id = 1, key = "bookmark", value = "标题", url = "https://a.com")),
        )
        val viewModel = WebViewModel(historyRepo, FakeMediaRepository())

        viewModel.init("https://b.com")

        assertNull(viewModel.bookmark.value)
    }

    @Test
    fun `toggle bookmark - creates bookmark when absent`() = runTest {
        val historyRepo = FakeHistoryRepository()
        val viewModel = WebViewModel(historyRepo, FakeMediaRepository())
        viewModel.init("https://a.com")

        viewModel.toggleBookmark("标题", "https://a.com")

        assertEquals(listOf("标题" to "https://a.com"), historyRepo.setBookmarks)
        assertTrue(historyRepo.deleted.isEmpty())
    }

    @Test
    fun `toggle bookmark - deletes bookmark when present`() = runTest {
        val existing = History(id = 1, key = "bookmark", value = "标题", url = "https://a.com")
        val historyRepo = FakeHistoryRepository(bookmarks = listOf(existing))
        val viewModel = WebViewModel(historyRepo, FakeMediaRepository())
        viewModel.init("https://a.com")

        viewModel.toggleBookmark("标题", "https://a.com")

        assertEquals(listOf(existing), historyRepo.deleted)
        assertTrue(historyRepo.setBookmarks.isEmpty())
    }

    @Test
    fun `record browse visit - delegates to history repository`() = runTest {
        val historyRepo = FakeHistoryRepository()
        val viewModel = WebViewModel(historyRepo, FakeMediaRepository())

        viewModel.recordBrowseVisit("标题", "https://a.com")

        assertEquals(listOf("标题" to "https://a.com"), historyRepo.visits)
    }

    @Test
    fun `save image with null extra - reports false without touching repository`() = runTest {
        val mediaRepo = FakeMediaRepository()
        val viewModel = WebViewModel(FakeHistoryRepository(), mediaRepo)
        var result: Boolean? = null

        viewModel.saveImage(null) { result = it }

        assertFalse(result ?: true)
        assertTrue(mediaRepo.urlSaves.isEmpty())
        assertTrue(mediaRepo.base64Saves.isEmpty())
    }

    @Test
    fun `save image with http url - goes through url save`() = runTest {
        val mediaRepo = FakeMediaRepository()
        val viewModel = WebViewModel(FakeHistoryRepository(), mediaRepo)
        var result: Boolean? = null

        viewModel.saveImage("https://img.example.com/a.png") { result = it }

        assertEquals(listOf("https://img.example.com/a.png"), mediaRepo.urlSaves)
        assertTrue(mediaRepo.base64Saves.isEmpty())
        assertTrue(result ?: false)
    }

    @Test
    fun `save image with base64 - goes through base64 save`() = runTest {
        val mediaRepo = FakeMediaRepository()
        val viewModel = WebViewModel(FakeHistoryRepository(), mediaRepo)
        var result: Boolean? = null
        val base64 = "iVBORw0KGgoAAAANSUhEUg=="

        viewModel.saveImage(base64) { result = it }

        assertEquals(listOf(base64), mediaRepo.base64Saves)
        assertTrue(mediaRepo.urlSaves.isEmpty())
        assertTrue(result ?: false)
    }
}

private class FakeHistoryRepository(
    bookmarks: List<History> = emptyList(),
) : HistoryRepository {

    private val bookmarkFlow = MutableStateFlow(bookmarks)
    val setBookmarks = mutableListOf<Pair<String, String>>()
    val visits = mutableListOf<Pair<String, String>>()
    val deleted = mutableListOf<History>()

    override fun observeBookmarks(): Flow<List<History>> = bookmarkFlow

    override fun observeBrowseHistory(): Flow<List<History>> = emptyFlow()

    override fun observeSearchHistory(): Flow<List<History>> = emptyFlow()

    override suspend fun setBookmark(value: String, url: String) {
        setBookmarks += value to url
    }

    override suspend fun recordBrowseVisit(value: String, url: String) {
        visits += value to url
    }

    override suspend fun recordSearchQuery(value: String) = Unit

    override suspend fun deleteHistory(history: History) {
        deleted += history
    }
}

private class FakeMediaRepository : MediaRepository {

    val urlSaves = mutableListOf<String>()
    val base64Saves = mutableListOf<String>()

    override suspend fun saveImageToAlbum(url: String): MediaSaveResult {
        urlSaves += url
        return MediaSaveResult(success = true)
    }

    override suspend fun saveImageToAlbum(imageBytes: ByteArray): MediaSaveResult =
        MediaSaveResult(success = true)

    override suspend fun saveBase64ImageToAlbum(base64: String): MediaSaveResult {
        base64Saves += base64
        return MediaSaveResult(success = true)
    }

    override suspend fun saveVideoToAlbum(filePath: String): MediaSaveResult =
        error("unexpected call in WebViewModelTest")

    override suspend fun notifyMediaAdded(filePath: String): MediaSaveResult =
        error("unexpected call in WebViewModelTest")

    override suspend fun createImageUri(): String = error("unexpected call in WebViewModelTest")

    override suspend fun finishImageUri(uri: String) = Unit

    override suspend fun deleteImageUri(uri: String) = Unit
}
