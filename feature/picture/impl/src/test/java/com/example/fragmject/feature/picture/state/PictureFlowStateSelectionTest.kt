package com.example.fragmject.feature.picture.state

import com.example.fragmject.core.domain.repository.AlbumGroup
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.repository.PictureEditRepository
import com.example.fragmject.core.domain.repository.PictureSelectionRepository
import com.example.fragmject.core.domain.result.MediaSaveResult
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 选中态的「进程重建可恢复」契约。
 *
 * 回归点：选择器/预览页的返回栈会被 Navigation 3 恢复，但选中列表原本只活在进程内，
 * 重启后预览底部会从「取消选择」退回「选择」、缩略图条没有勾选标记，「完成」还会把
 * 空列表报给调用方。本用例只覆盖纯 String 的选中逻辑，不触碰 `Uri`。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PictureFlowStateSelectionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `进程重建后从持久记录恢复选中态`() = runTest {
        val selection = FakeSelectionRepository(stored = listOf(URI_A, URI_B))
        val flowState = createFlowState(selection)

        flowState.restorePersistedSelection()

        assertEquals(listOf(URI_A, URI_B), flowState.selectedUris.value)
        assertEquals(setOf(URI_A, URI_B), flowState.selectedUriSet.value)
    }

    @Test
    fun `内存已有选中时不被持久记录覆盖`() = runTest {
        val selection = FakeSelectionRepository()
        val flowState = createFlowState(selection)
        flowState.toggleSelection(URI_A)
        // 模拟库里还是另一批数据（例如写库延迟/被别处覆盖），恢复不能盖掉内存里更新的选中
        selection.stored = listOf(URI_B)

        flowState.restorePersistedSelection()

        assertEquals(listOf(URI_A), flowState.selectedUris.value)
    }

    @Test
    fun `选中与取消选中都会落库`() = runTest {
        val selection = FakeSelectionRepository()
        val flowState = createFlowState(selection)

        flowState.toggleSelection(URI_A)
        advanceUntilIdle()
        assertEquals(listOf(URI_A), selection.stored)

        flowState.toggleSelection(URI_A)
        advanceUntilIdle()
        assertEquals(emptyList<String>(), selection.stored)
    }

    @Test
    fun `reset 同时清空内存与持久记录`() = runTest {
        val selection = FakeSelectionRepository(stored = listOf(URI_A))
        val flowState = createFlowState(selection)

        flowState.reset()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), flowState.selectedUris.value)
        assertEquals(emptyList<String>(), selection.stored)
    }

    private fun createFlowState(selection: PictureSelectionRepository) = PictureFlowState(
        mediaRepository = NoopMediaRepository(),
        albumRepository = NoopAlbumRepository(),
        editedUriResolver = EditedUriResolver(NoopEditRepository()),
        selectionRepository = selection,
    )

    private class FakeSelectionRepository(
        var stored: List<String> = emptyList(),
    ) : PictureSelectionRepository {
        override suspend fun loadSelectedUris(): List<String> = stored

        override suspend fun saveSelectedUris(uris: List<String>) {
            stored = uris
        }
    }

    private class NoopAlbumRepository : AlbumRepository {
        override suspend fun queryAlbums(): List<AlbumGroup> = emptyList()
    }

    private class NoopEditRepository : PictureEditRepository {
        override suspend fun recordReplacement(oldUri: String, newUri: String) = Unit

        override suspend fun findReplacement(oldUri: String): String? = null
    }

    private class NoopMediaRepository : MediaRepository {
        override suspend fun saveImageToAlbum(url: String) = MediaSaveResult(success = false)
        override suspend fun saveImageToAlbum(imageBytes: ByteArray) = MediaSaveResult(success = false)
        override suspend fun saveBase64ImageToAlbum(base64: String) = MediaSaveResult(success = false)
        override suspend fun saveVideoToAlbum(filePath: String) = MediaSaveResult(success = false)
        override suspend fun notifyMediaAdded(filePath: String) = MediaSaveResult(success = false)
        override suspend fun createImageUri(): String = ""
        override suspend fun finishImageUri(uri: String): Boolean = false
        override suspend fun deleteImageUri(uri: String) = Unit
    }

    private companion object {
        const val URI_A = "content://media/external/images/media/1"
        const val URI_B = "content://media/external/images/media/2"
    }
}
