package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import com.example.fragmject.core.image.editor.ImageEditorEngine
import com.example.fragmject.core.image.editor.SaveResult
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 保存编排契约：ViewModel 只负责串行化与状态，保存动作由调用方提供。
 *
 * 因为不再接收 `produce: () -> Bitmap?`，这些用例可以在 JVM 上完整覆盖
 * 成功 / 失败 / 异常 / 取消 / 重复点击，无需构造 `Bitmap`。
 */
class PictureEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `save success delivers the result and resets isSaving`() = runTest {
        val viewModel = PictureEditorViewModel(NoopEngine())
        var delivered: SaveResult? = null
        var failed = false

        viewModel.save(
            saveAction = { SaveResult("path", "uri") },
            onSuccess = { delivered = it },
            onError = { failed = true },
        )

        assertEquals(SaveResult("path", "uri"), delivered)
        assertFalse(failed)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `save failure reports error and resets isSaving`() = runTest {
        val viewModel = PictureEditorViewModel(NoopEngine())
        var delivered: SaveResult? = null
        var failed = false

        viewModel.save(
            saveAction = { null },
            onSuccess = { delivered = it },
            onError = { failed = true },
        )

        assertTrue(failed)
        assertEquals(null, delivered)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `repeated taps while saving are ignored`() = runTest {
        val viewModel = PictureEditorViewModel(NoopEngine())
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        var delivered: SaveResult? = null

        viewModel.save(
            saveAction = {
                calls++
                gate.await()
                SaveResult("first", "uri")
            },
            onSuccess = { delivered = it },
            onError = {},
        )
        assertTrue(viewModel.isSaving.value)

        viewModel.save(
            saveAction = {
                calls++
                SaveResult("second", "uri")
            },
            onSuccess = { delivered = it },
            onError = {},
        )
        assertEquals(1, calls)

        gate.complete(Unit)
        assertEquals("first", delivered?.path)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `exception is mapped to error instead of crashing`() = runTest {
        val viewModel = PictureEditorViewModel(NoopEngine())
        var failed = false

        viewModel.save(
            saveAction = { throw IllegalStateException("disk full") },
            onSuccess = {},
            onError = { failed = true },
        )

        assertTrue(failed)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `cancellation is rethrown without reporting an error`() = runTest {
        val viewModel = PictureEditorViewModel(NoopEngine())
        var failed = false

        viewModel.save(
            saveAction = { throw CancellationException("cancelled") },
            onSuccess = {},
            onError = { failed = true },
        )

        assertFalse("取消不能降级成保存失败", failed)
        assertFalse(viewModel.isSaving.value)
    }
}

/** 保存动作全部由测试 lambda 提供，引擎只需满足构造签名。 */
private class NoopEngine : ImageEditorEngine {
    override val saveProgress: Flow<Float> = emptyFlow()
    override suspend fun loadBitmap(path: String?, uri: String?, targetWidth: Int): Bitmap? = null
    override suspend fun save(bitmap: Bitmap): SaveResult? = null
}
