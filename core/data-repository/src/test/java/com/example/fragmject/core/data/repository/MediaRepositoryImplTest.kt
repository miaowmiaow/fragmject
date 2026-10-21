package com.example.fragmject.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.result.SimpleResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 媒体保存的平台契约（Robolectric）：
 * 输入缺失或 MediaStore 不可用时必须安全失败，不能抛出、也不能返回 success。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36]) // Robolectric 4.16 最高支持 API 36，项目 targetSdk 37 需显式下探
class MediaRepositoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun repository() = MediaRepositoryImpl(context, FakeDownloadRepository())

    @Test
    fun `saving a missing video fails fast`() = runTest {
        val result = repository().saveVideoToAlbum("/definitely/missing.mp4")

        assertFalse(result.success)
        assertEquals("", result.uri)
    }

    @Test
    fun `notifying a missing media file fails fast`() = runTest {
        val result = repository().notifyMediaAdded("/definitely/missing.jpg")

        assertFalse(result.success)
        assertEquals("", result.uri)
    }

    @Test
    fun `create finish and delete image uri do not throw`() = runTest {
        val repository = repository()

        val uri = repository.createImageUri()
        repository.finishImageUri(uri)
        repository.deleteImageUri(uri)
    }

    @Test
    fun `saving raw bytes always returns a result`() = runTest {
        val result = repository().saveImageToAlbum(byteArrayOf(1, 2, 3))

        // Robolectric 下没有真实 MediaStore provider，保存会失败；关键是走完清理路径且不抛异常
        assertNotNull(result)
    }
}

private class FakeDownloadRepository : DownloadRepository {
    override suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String>,
    ): SimpleResult = SimpleResult(false, "unused")
}
