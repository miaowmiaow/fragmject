package com.example.fragmject.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 相册查询的平台契约（Robolectric）：
 * 空 MediaStore 也要返回「所有照片」分组，保证选择器有稳定入口，而不是空列表。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36]) // Robolectric 4.16 最高支持 API 36，项目 targetSdk 37 需显式下探
class AlbumRepositoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `empty media store still yields the all photos bucket`() = runTest {
        val groups = AlbumRepositoryImpl(context).queryAlbums()

        assertEquals(1, groups.size)
        val all = groups.single()
        assertEquals("所有照片", all.name)
        assertTrue(all.images.isEmpty())
        assertEquals("没有图片时封面为空串", "", all.coverUri)
    }
}
