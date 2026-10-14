package com.example.fragmject.app.navigation

import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.home.MainNavKey
import com.example.fragmject.feature.search.SearchNavKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkGuardTest {

    @Test
    fun `web deep link accepts http and https only`() {
        assertTrue(isAllowedDeepLinkTarget(WebNavKey("https://www.wanandroid.com/blog/show/2")))
        assertTrue(isAllowedDeepLinkTarget(WebNavKey("http://www.baidu.com")))
    }

    @Test
    fun `web deep link rejects unsafe or bundled targets`() {
        listOf(
            "javascript:alert(1)",
            "content://com.example.fragment.project.provider/x",
            "file:///data/data/com.example.fragment.project/databases/db",
            "file:///android_asset/privacy_policy.html",
            "",
        ).forEach { assertFalse("should reject: $it", isAllowedDeepLinkTarget(WebNavKey(it))) }
    }

    @Test
    fun `non web targets are not affected`() {
        assertTrue(isAllowedDeepLinkTarget(MainNavKey))
        assertTrue(isAllowedDeepLinkTarget(SearchNavKey("kotlin")))
    }
}
