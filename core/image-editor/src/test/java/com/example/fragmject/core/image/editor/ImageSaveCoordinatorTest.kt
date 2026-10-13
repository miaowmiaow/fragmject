package com.example.fragmject.core.image.editor

import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.result.MediaSaveResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ImageSaveCoordinator] 保存业务规则测试（纯 JVM）。
 *
 * 经 [ImageSaveCoordinator.saveInternal] 直接验证三条规则，
 * 不依赖真实 android.graphics.Bitmap：
 * 1. 尺寸非法（宽或高 ≤ 1）拒绝；
 * 2. 编码失败拒绝；
 * 3. 落盘 uri 非空才算成功。
 */
class ImageSaveCoordinatorTest {

    @Test
    fun `rejects invalid small bitmap`() = runTest {
        val coordinator = ImageSaveCoordinator(FakeMediaRepository())
        val result = coordinator.saveInternal(1, 1) { byteArrayOf(1, 2, 3) }
        assertNull(result)
    }

    @Test
    fun `rejects when encoding fails`() = runTest {
        val coordinator = ImageSaveCoordinator(FakeMediaRepository())
        val result = coordinator.saveInternal(100, 100) { null }
        assertNull(result)
    }

    @Test
    fun `rejects when save returns blank uri`() = runTest {
        val repo = FakeMediaRepository(MediaSaveResult(success = true, path = "/tmp/x", uri = ""))
        val coordinator = ImageSaveCoordinator(repo)
        val result = coordinator.saveInternal(100, 100) { byteArrayOf(1) }
        assertNull(result)
    }

    @Test
    fun `rejects when save fails`() = runTest {
        val repo = FakeMediaRepository(MediaSaveResult(success = false))
        val coordinator = ImageSaveCoordinator(repo)
        val result = coordinator.saveInternal(100, 100) { byteArrayOf(1) }
        assertNull(result)
    }

    @Test
    fun `returns result on success`() = runTest {
        val repo = FakeMediaRepository(
            MediaSaveResult(success = true, path = "/tmp/x.png", uri = "content://x")
        )
        val coordinator = ImageSaveCoordinator(repo)
        val result = coordinator.saveInternal(100, 100) { byteArrayOf(1, 2) }
        assertEquals(SaveResult("/tmp/x.png", "content://x"), result)
    }
}

private class FakeMediaRepository(
    private val saveResult: MediaSaveResult = MediaSaveResult(success = false),
) : MediaRepository {
    override suspend fun saveImageToAlbum(url: String): MediaSaveResult = saveResult
    override suspend fun saveImageToAlbum(imageBytes: ByteArray): MediaSaveResult = saveResult
    override suspend fun saveBase64ImageToAlbum(base64: String): MediaSaveResult = saveResult
    override suspend fun saveVideoToAlbum(filePath: String): MediaSaveResult = saveResult
    override suspend fun notifyMediaAdded(filePath: String): MediaSaveResult = saveResult
    override suspend fun createImageUri(): String = ""
    override suspend fun finishImageUri(uri: String) = Unit
    override suspend fun deleteImageUri(uri: String) = Unit
}
