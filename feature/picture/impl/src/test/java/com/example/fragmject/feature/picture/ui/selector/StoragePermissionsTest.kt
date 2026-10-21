package com.example.fragmject.feature.picture.ui.selector

import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * 相册读取权限的版本分档。
 *
 * 断言用权限名字面量而不是 `Manifest.permission.*`：JVM 单测里的 android.jar 只是桩，
 * 用常量会让断言依赖「桩字段是否被内联」，属于测试自身的脆弱点。
 */
class StoragePermissionsTest {

    @Test
    fun `api 34 asks for images plus partial photo access`() {
        assertArrayEquals(
            arrayOf(
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
            ),
            storagePermissionsForApi(34),
        )
    }

    @Test
    fun `api 33 asks for images only`() {
        assertArrayEquals(
            arrayOf("android.permission.READ_MEDIA_IMAGES"),
            storagePermissionsForApi(33),
        )
    }

    @Test
    fun `api below 33 falls back to external storage`() {
        assertArrayEquals(
            arrayOf("android.permission.READ_EXTERNAL_STORAGE"),
            storagePermissionsForApi(26),
        )
        assertArrayEquals(
            arrayOf("android.permission.READ_EXTERNAL_STORAGE"),
            storagePermissionsForApi(32),
        )
    }
}
