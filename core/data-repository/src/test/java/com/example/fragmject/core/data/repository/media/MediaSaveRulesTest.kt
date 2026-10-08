package com.example.fragmject.core.data.repository.media

import com.example.fragmject.core.domain.media.ImageHandle
import com.example.fragmject.core.domain.result.MediaSaveResult
import com.example.fragmject.core.domain.repository.MediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [MediaSaveRules] 测试：验证端口承担的业务规则。
 *
 * 覆盖此前 UI 侧各自判断导致的不一致：
 * - 落盘返回空 uri 时必须判失败（曾出现 success=true 但 uri 为空）；
 * - 尺寸非法的位图不得进入保存流程（曾出现 1×1 空白图被写入相册）；
 * - 编码失败不得继续落盘。
 */
class MediaSaveRulesTest {

    private class FakeMediaRepository(
        private val result: MediaSaveResult,
    ) : MediaRepository {
        var lastBytes: ByteArray? = null

        override suspend fun saveImageToAlbum(url: String): MediaSaveResult = result
        override suspend fun saveImageToAlbum(imageBytes: ByteArray): MediaSaveResult {
            lastBytes = imageBytes
            return result
        }
        override suspend fun saveBase64ImageToAlbum(base64: String): MediaSaveResult = result
        override suspend fun saveVideoToAlbum(filePath: String): MediaSaveResult = result
        override suspend fun notifyMediaAdded(filePath: String): MediaSaveResult = result
        override suspend fun createImageUri(): String = ""
        override suspend fun finishImageUri(uri: String) = Unit
        override suspend fun deleteImageUri(uri: String) = Unit
    }

    private class FakeImageHandle(
        override val width: Int,
        override val height: Int,
        private val bytes: ByteArray? = ByteArray(1),
    ) : ImageHandle {
        override suspend fun obtainPngBytes(): ByteArray? = bytes
    }

    private fun rules(result: MediaSaveResult): Pair<MediaSaveRules, FakeMediaRepository> {
        val repo = FakeMediaRepository(result)
        return MediaSaveRules(repo) to repo
    }

    private val saved = MediaSaveResult(success = true, path = "/p/a.png", uri = "content://a")

    @Test
    fun `save returns edited image when persisted uri is not blank`() = runTest {
        val (rules, _) = rules(saved)

        val edited = rules.save(FakeImageHandle(100, 100))

        assertEquals("/p/a.png", edited?.path)
        assertEquals("content://a", edited?.uriString)
    }

    @Test
    fun `save fails when persisted uri is blank even if success flag is true`() = runTest {
        val (rules, _) = rules(MediaSaveResult(success = true, path = "/p/a.png", uri = ""))

        assertNull(rules.save(FakeImageHandle(100, 100)))
    }

    @Test
    fun `save rejects degenerate bitmap before persisting`() = runTest {
        val (rules, repo) = rules(saved)

        assertNull(rules.save(FakeImageHandle(1, 1)))
        assertNull(repo.lastBytes)
    }

    @Test
    fun `save fails when encoding fails`() = runTest {
        val (rules, repo) = rules(saved)

        assertNull(rules.save(FakeImageHandle(100, 100, bytes = null)))
        assertNull(repo.lastBytes)
    }

    @Test
    fun `save progress reaches completion`() = runTest {
        val (rules, _) = rules(saved)

        rules.save(FakeImageHandle(100, 100))

        assertEquals(1f, rules.saveProgress.first(), 0f)
    }
}
