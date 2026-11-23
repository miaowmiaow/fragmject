package com.example.fragmject.core.android.platform.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 日志脱敏：URL 去 query/fragment、路径只留文件名。
 *
 * 由来：第三方图床 URL 带 `x-signature` / `token`，文件绝对路径带包名，
 * 这些都会随 logcat 被 `adb bugreport` 带出去。
 */
class LogSanitizerTest {

    @Test
    fun `URL 去掉查询串与签名`() {
        assertEquals(
            "https://p9.byteimg.com/tos-cn-i/a.webp",
            LogSanitizer.url("https://p9.byteimg.com/tos-cn-i/a.webp?x-signature=abc%3D&x-expires=1"),
        )
        assertEquals(
            "https://www.wanandroid.com/user/login",
            LogSanitizer.url("https://www.wanandroid.com/user/login?username=a&password=b"),
        )
    }

    @Test
    fun `URL 去掉 fragment 并截断超长部分`() {
        assertEquals("https://a.com/p", LogSanitizer.url("https://a.com/p#section"))
        val longUrl = "https://a.com/" + "x".repeat(300)
        val sanitized = LogSanitizer.url(longUrl)
        assertTrue(sanitized.length <= 120)
        assertTrue(sanitized.startsWith("https://a.com/"))
    }

    @Test
    fun `空值返回占位符而不是空串`() {
        assertEquals("-", LogSanitizer.url(null))
        assertEquals("-", LogSanitizer.url("   "))
        assertEquals("-", LogSanitizer.path(null))
        assertEquals("-", LogSanitizer.path(""))
    }

    @Test
    fun `路径只保留文件名`() {
        assertEquals(
            "image_1795402723839.webp",
            LogSanitizer.path("/storage/emulated/0/Android/data/com.example.fragment.project/cache/Pictures/image_1795402723839.webp"),
        )
        assertEquals("no-slash.txt", LogSanitizer.path("no-slash.txt"))
        assertFalse(LogSanitizer.path("/data/user/0/com.example.fragment.project/files/a.db").contains("com.example"))
    }
}
