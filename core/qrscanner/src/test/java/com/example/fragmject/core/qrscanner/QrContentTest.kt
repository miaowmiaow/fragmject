package com.example.fragmject.core.qrscanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrContentTest {

    @Test
    fun `URL 类型且解析出 URL 时取解析后的 URL`() {
        assertEquals(
            "https://example.com/a",
            resolveQrContent(isUrl = true, url = "https://example.com/a", rawValue = "raw-text"),
        )
    }

    @Test
    fun `URL 类型但未解析出 URL 时回退到原始值`() {
        assertEquals(
            "raw-text",
            resolveQrContent(isUrl = true, url = null, rawValue = "raw-text"),
        )
    }

    @Test
    fun `非 URL 类型时直接取原始值，即使存在 url 字段也忽略`() {
        assertEquals(
            "plain-text",
            resolveQrContent(isUrl = false, url = "https://ignored.example", rawValue = "plain-text"),
        )
    }

    @Test
    fun `原始值为空且非 URL 时返回 null，调用方据此不回调`() {
        assertNull(resolveQrContent(isUrl = false, url = null, rawValue = null))
    }

    @Test
    fun `URL 类型且两者都为空时返回 null`() {
        assertNull(resolveQrContent(isUrl = true, url = null, rawValue = null))
    }
}
