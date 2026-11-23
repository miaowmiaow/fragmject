package com.example.fragmject.core.android.platform.file

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * MIME 推断：文件头优先于文件名。
 *
 * 由来（2026-11-23 真机）：保存图片时把无扩展名的下载缓存交给 MIME 推断，
 * 旧实现先按扩展名（必然落空）再走 MediaMetadataRetriever（面向音视频，对图片必抛
 * `setDataSource failed` 并刷日志），图片类型只能靠最后的魔数兜底。
 * 现在媒体类型由文件头直出，不再依赖文件名。
 */
class FileMimeUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `无扩展名的图片按文件头识别`() {
        assertEquals("image/jpeg", mimeOf("image_1", 0xFF, 0xD8, 0xFF, 0xE0))
        assertEquals("image/png", mimeOf("image_2", 0x89, 0x50, 0x4E, 0x47))
        assertEquals("image/gif", mimeOf("image_3", 0x47, 0x49, 0x46, 0x38, 0x39, 0x61))
        assertEquals("image/webp", mimeOf("image_4", 0x52, 0x49, 0x46, 0x46))
    }

    @Test
    fun `扩展名与文件头冲突时以文件头为准`() {
        // CDN 按 Accept 转码后常出现「名字是 .jpg、内容其实是 webp」的情况
        assertEquals("image/webp", mimeOf("wrong.jpg", 0x52, 0x49, 0x46, 0x46))
    }

    private fun mimeOf(name: String, vararg magic: Int): String {
        val file = tempFolder.newFile(name)
        file.writeBytes(ByteArray(magic.size) { magic[it].toByte() } + ByteArray(16))
        return FileUtils.getFileMimeType(file)
    }
}
