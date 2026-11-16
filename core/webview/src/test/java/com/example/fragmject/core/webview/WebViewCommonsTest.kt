package com.example.fragmject.core.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewCommonsTest {

    @Test
    fun `http and https are accepted case-insensitively`() {
        assertTrue(WebViewCommons.isHttpUrl("http://www.wanandroid.com"))
        assertTrue(WebViewCommons.isHttpUrl("https://www.wanandroid.com/a?b=1"))
        assertTrue(WebViewCommons.isHttpUrl("HTTPS://WANANDROID.COM"))
    }

    @Test
    fun `non http schemes are rejected as http urls`() {
        listOf(
            "javascript:alert(1)",
            "content://com.example.provider/file",
            "file:///data/data/app/secret",
            "file:///android_asset/privacy_policy.html",
            "intent://scan/#Intent;scheme=zxing;end",
            "ftp://example.com/a",
            "wanandroid.com",
            "http:/missing-slash",
            " https://leading-space.com",
            "",
        ).forEach { assertFalse("should reject: $it", WebViewCommons.isHttpUrl(it)) }
    }

    @Test
    fun `loadable urls allow http https and bundled assets only`() {
        assertTrue(WebViewCommons.isLoadableUrl("https://wanandroid.com"))
        assertTrue(WebViewCommons.isLoadableUrl("http://wanandroid.com"))
        assertTrue(WebViewCommons.isLoadableUrl("file:///android_asset/html/privacy_policy.html"))
    }

    @Test
    fun `loadable urls reject dangerous or unsupported targets`() {
        listOf(
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            "content://com.example.provider/file",
            "file:///data/data/app/secret",
            "file:///sdcard/a.html",
            "file:///android_asset/",
            "file:///android_asset/../secret.html",
            "file:///android_asset/%2e%2e/secret.html",
            "file:///android_asset/%2E%2E/secret.html",
            "about:blank",
            "",
        ).forEach { assertFalse("should reject: $it", WebViewCommons.isLoadableUrl(it)) }
    }

    @Test
    fun `asset url maps to relative asset path`() {
        assertEquals(
            "html/privacy_policy.html",
            WebViewCommons.assetPathOf("file:///android_asset/html/privacy_policy.html"),
        )
        assertEquals(
            "html/privacy_policy.css",
            WebViewCommons.assetPathOf("file:///android_asset/html/privacy_policy.css"),
        )
        assertEquals("foo.js", WebViewCommons.assetPathOf("file:///android_asset/foo.js"))
    }

    @Test
    fun `asset url strips query and fragment`() {
        assertEquals(
            "html/a.html",
            WebViewCommons.assetPathOf("file:///android_asset/html/a.html?v=1#top"),
        )
    }

    @Test
    fun `non asset or unsafe urls yield no asset path`() {
        listOf(
            "https://wanandroid.com",
            "file:///data/data/app/secret",
            "file:///sdcard/a.html",
            "file:///android_asset/",
            "file:///android_asset//etc/passwd",
            "file:///android_asset/../secret.html",
            "file:///android_asset/html/../../secret.html",
            "file:///android_asset/%2e%2e/secret.html",
            "file:///android_asset/%2E%2E/secret.html",
            "",
        ).forEach { assertNull("should yield no path: $it", WebViewCommons.assetPathOf(it)) }
    }

    @Test
    fun `external intent allowed only for gesture-triggered tel mailto sms`() {
        assertTrue(WebViewCommons.isExternalIntentAllowed("tel:10086", hasUserGesture = true))
        assertTrue(WebViewCommons.isExternalIntentAllowed("mailto:a@b.c", hasUserGesture = true))
        assertTrue(WebViewCommons.isExternalIntentAllowed("SMS:10086", hasUserGesture = true))
    }

    @Test
    fun `external intent denied without user gesture`() {
        assertFalse(WebViewCommons.isExternalIntentAllowed("tel:10086", hasUserGesture = false))
        assertFalse(WebViewCommons.isExternalIntentAllowed("mailto:a@b.c", hasUserGesture = false))
    }

    @Test
    fun `external intent denies intent and custom app schemes even with gesture`() {
        listOf(
            "intent://scan/#Intent;scheme=zxing;end",
            "intent:#Intent;component=com.example/.Internal;end",
            "weixin://dl/business",
            "alipays://platformapi/startapp",
            "market://details?id=com.example",
            "javascript:alert(1)",
            "content://com.example.provider/file",
            "file:///data/data/app/secret",
            "",
        ).forEach {
            assertFalse("should deny: $it", WebViewCommons.isExternalIntentAllowed(it, hasUserGesture = true))
        }
    }
}
