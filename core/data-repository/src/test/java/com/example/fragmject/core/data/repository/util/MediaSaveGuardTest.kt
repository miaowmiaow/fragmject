package com.example.fragmject.core.data.repository.util

import com.example.fragmject.core.domain.result.MediaSaveResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class MediaSaveGuardTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `successful save is returned unchanged`() = runTest {
        val expected = MediaSaveResult(success = true, path = "/p", uri = "content://x")
        val failures = mutableListOf<Throwable>()

        val result = guardedMediaSave(onFailure = { failures += it }) { expected }

        assertEquals(expected, result)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `ordinary exception becomes a failed result and is reported`() = runTest {
        val cause = IOException("disk full")
        val failures = mutableListOf<Throwable>()

        val result = guardedMediaSave(onFailure = { failures += it }) { throw cause }

        assertEquals(MediaSaveResult(success = false), result)
        assertSame(cause, failures.single())
    }

    @Test
    fun `cancellation exception is rethrown and not reported as a failure`() = runTest {
        val cancellation = CancellationException("cancelled")
        val failures = mutableListOf<Throwable>()

        try {
            guardedMediaSave(onFailure = { failures += it }) { throw cancellation }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertTrue("取消不应被记录为保存失败", failures.isEmpty())
    }

    @Test
    fun `cancelling the caller cancels the job instead of yielding a failed result`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val failures = mutableListOf<Throwable>()
        var result: MediaSaveResult? = null

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            result = guardedMediaSave(onFailure = { failures += it }) {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull("取消后不应产出任何结果", result)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `temporary file is deleted and the original exception rethrown on failure`() {
        val file = temporaryFolder.newFile("download")
        val cause = IOException("stream broke")

        try {
            file.discardOnFailure { throw cause }
            fail("Failure must propagate")
        } catch (actual: IOException) {
            assertSame(cause, actual)
        }
        assertFalse(file.exists())
    }

    @Test
    fun `temporary file is deleted when preparation is cancelled`() {
        val file = temporaryFolder.newFile("download")
        val cancellation = CancellationException("cancelled")

        try {
            file.discardOnFailure { throw cancellation }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertFalse(file.exists())
    }

    @Test
    fun `temporary file is kept after successful preparation`() {
        val file = temporaryFolder.newFile("download")

        val value = file.discardOnFailure { "ready" }

        assertEquals("ready", value)
        assertTrue("交接给 MediaStore 之前不得提前删除", file.exists())
    }
}
