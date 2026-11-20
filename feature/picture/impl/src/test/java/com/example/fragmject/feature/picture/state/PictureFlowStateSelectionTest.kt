package com.example.fragmject.feature.picture.state

import com.example.fragmject.core.domain.repository.AlbumGroup
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.repository.PictureEditRepository
import com.example.fragmject.core.domain.repository.PictureSelectionRepository
import com.example.fragmject.core.domain.result.MediaSaveResult
import androidx.core.net.toUri
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 选中态的「进程重建可恢复」契约。
 *
 * 回归点：选择器/预览页的返回栈会被 Navigation 3 恢复，但选中列表原本只活在进程内，
 * 重启后预览底部会从「取消选择」退回「选择」、缩略图条没有勾选标记，「完成」还会把
 * 空列表报给调用方。本用例只覆盖纯 String 的选中逻辑，不触碰 `Uri`。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `进程重建后从持久 URI 完成拍照`() = runTest {
        val selection = FakeSelectionRepository()
        val media = FakeMediaRepository()
        val originalFlow = createFlowState(selection, media)
        val uri = originalFlow.createTakePictureUri()
        assertEquals(CAPTURE_URI, uri.toString())
        assertEquals(
            PictureSelectionRepository.CaptureDisposition.AWAITING_RESULT,
            selection.pendingCapture?.disposition,
        )

        val restoredFlow = createFlowState(selection, media)
        assertEquals(true, restoredFlow.finishTakePictureUri())

        assertEquals(listOf(CAPTURE_URI), media.finishedUris)
        assertEquals(null, selection.pendingCapture)
    }

    @Test
    fun `无法持久化拍照 URI 时不启动拍照并清理已创建记录`() = runTest {
        val selection = FakeSelectionRepository(pendingCaptureWriteSucceeds = false)
        val media = FakeMediaRepository()
        val flowState = createFlowState(selection, media)

        assertEquals(null, flowState.createTakePictureUri())
        assertEquals(listOf(CAPTURE_URI), media.deletedUris)
    }

    @Test
    fun `发布失败且删除失败时保留 URI 供重试`() = runTest {
        val selection = FakeSelectionRepository()
        val media = FakeMediaRepository(finishResult = false, deleteResult = false)
        val flowState = createFlowState(selection, media)
        val uri = flowState.createTakePictureUri()!!

        assertEquals(false, flowState.finishTakePictureUri())

        assertEquals(
            PictureSelectionRepository.PendingCapture(uri.toString(), PictureSelectionRepository.CaptureDisposition.DELETE),
            selection.pendingCapture,
        )
        assertEquals(uri, flowState.takePictureUri.value)
    }

    @Test
    fun `同进程再次拍照时重试清理失败的 DELETE 记录`() = runTest {
        val selection = FakeSelectionRepository()
        val media = FakeMediaRepository(finishResult = false, deleteResult = false)
        val flowState = createFlowState(selection, media)
        val originalUri = flowState.createTakePictureUri()!!

        assertEquals(false, flowState.finishTakePictureUri())
        assertEquals(PictureSelectionRepository.CaptureDisposition.DELETE, selection.pendingCapture?.disposition)

        media.deleteResult = true
        val nextUri = flowState.createTakePictureUri()

        assertEquals(CAPTURE_URI, nextUri.toString())
        assertEquals(listOf(originalUri.toString(), originalUri.toString()), media.deletedUris)
        assertEquals(PictureSelectionRepository.CaptureDisposition.AWAITING_RESULT, selection.pendingCapture?.disposition)
    }

    @Test
    fun `仍在等待拍照结果时不覆盖现有 URI`() = runTest {
        val selection = FakeSelectionRepository()
        val media = FakeMediaRepository()
        val flowState = createFlowState(selection, media)

        val uri = flowState.createTakePictureUri()!!
        assertEquals(null, flowState.createTakePictureUri())
        assertEquals(uri, flowState.takePictureUri.value)
        assertEquals(emptyList<String>(), media.deletedUris)
    }

    @Test
    fun `编辑替换关系持久化失败时保留原图`() = runTest {
        val selection = FakeSelectionRepository()
        val media = FakeMediaRepository()
        val editRepository = object : PictureEditRepository {
            override suspend fun recordReplacement(oldUri: String, newUri: String) {
                error("database unavailable")
            }

            override suspend fun findReplacement(oldUri: String): String? = null
        }
        val flowState = createFlowState(selection, media, editRepository)

        flowState.applyEditResult(URI_A.toUri(), URI_B.toUri())
        advanceUntilIdle()

        assertEquals(emptyList<String>(), media.deletedUris)
    }

    private fun createFlowState(
        selection: PictureSelectionRepository,
        media: FakeMediaRepository = FakeMediaRepository(),
        editRepository: PictureEditRepository = NoopEditRepository(),
    ) = PictureFlowState(
        mediaRepository = media,
        albumRepository = NoopAlbumRepository(),
        editedUriResolver = EditedUriResolver(editRepository),
        selectionRepository = selection,
    )

    private class FakeSelectionRepository(
        var stored: List<String> = emptyList(),
        var pendingCapture: PictureSelectionRepository.PendingCapture? = null,
        var pendingCaptureWriteSucceeds: Boolean = true,
    ) : PictureSelectionRepository {
        override suspend fun loadSelectedUris(): List<String> = stored

        override suspend fun saveSelectedUris(uris: List<String>) {
            stored = uris
        }

        override suspend fun loadPendingCapture(): PictureSelectionRepository.PendingCapture? = pendingCapture

        override suspend fun savePendingCapture(
            capture: PictureSelectionRepository.PendingCapture?,
        ): Boolean {
            if (!pendingCaptureWriteSucceeds) return false
            pendingCapture = capture
            return true
        }
    }

    private class NoopAlbumRepository : AlbumRepository {
        override suspend fun queryAlbums(): List<AlbumGroup> = emptyList()
    }

    private class NoopEditRepository : PictureEditRepository {
        override suspend fun recordReplacement(oldUri: String, newUri: String) = Unit

        override suspend fun findReplacement(oldUri: String): String? = null
    }

    private class FakeMediaRepository(
        var finishResult: Boolean = true,
        var deleteResult: Boolean = true,
    ) : MediaRepository {
        val finishedUris = mutableListOf<String>()
        val deletedUris = mutableListOf<String>()

        override suspend fun saveImageToAlbum(url: String) = MediaSaveResult(success = false)
        override suspend fun saveImageToAlbum(imageBytes: ByteArray) = MediaSaveResult(success = false)
        override suspend fun saveBase64ImageToAlbum(base64: String) = MediaSaveResult(success = false)
        override suspend fun saveVideoToAlbum(filePath: String) = MediaSaveResult(success = false)
        override suspend fun notifyMediaAdded(filePath: String) = MediaSaveResult(success = false)
        override suspend fun createImageUri(): String = CAPTURE_URI
        override suspend fun finishImageUri(uri: String): Boolean {
            finishedUris += uri
            return finishResult
        }
        override suspend fun deleteImageUri(uri: String): Boolean {
            deletedUris += uri
            return deleteResult
        }
    }

    private companion object {
        const val URI_A = "content://media/external/images/media/1"
        const val URI_B = "content://media/external/images/media/2"
        const val CAPTURE_URI = "content://media/external/images/media/capture"
    }
}
