package com.example.fragmject.core.android.platform.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 相册条目名补扩展名的边界。
 *
 * 由来（2026-11-23 真机）：下载回来的图片以 `image_<timestamp>` 这类无扩展名临时名
 * 交接给 MediaStore，部分 ROM 会因此拒绝 insert 或无法生成缩略图。这段逻辑改为
 * internal 后由单测锁住，避免回归只能靠真机发现。
 */
class MediaStoreUtilsDisplayNameTest {

    @Test
    fun `无扩展名时按 mime 补后缀`() {
        assertEquals("image_1.jpg", MediaStoreUtils.displayNameWithExtension("image_1", "image/jpeg"))
        assertEquals("image_1.png", MediaStoreUtils.displayNameWithExtension("image_1", "image/png"))
        assertEquals("image_1.webp", MediaStoreUtils.displayNameWithExtension("image_1", "image/webp"))
        assertEquals("movie.mp4", MediaStoreUtils.displayNameWithExtension("movie", "video/mp4"))
    }

    @Test
    fun `已有扩展名时保持原样`() {
        assertEquals("photo.png", MediaStoreUtils.displayNameWithExtension("photo.png", "image/jpeg"))
        assertEquals("a.b.webp", MediaStoreUtils.displayNameWithExtension("a.b.webp", "image/webp"))
    }

    @Test
    fun `未知 mime 或异常文件名不伪造后缀`() {
        assertEquals("image_1", MediaStoreUtils.displayNameWithExtension("image_1", "*/*"))
        assertEquals("blob", MediaStoreUtils.displayNameWithExtension("blob", "application/octet-stream"))
        // 以点结尾的文件名保持原样，不能补成 `image..jpg`
        assertEquals("image.", MediaStoreUtils.displayNameWithExtension("image.", "image/jpeg"))
    }
}
