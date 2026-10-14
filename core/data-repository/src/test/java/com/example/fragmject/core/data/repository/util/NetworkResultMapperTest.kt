package com.example.fragmject.core.data.repository.util

import com.example.fragmject.core.data.contract.model.ApiCode
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.domain.result.getOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class NetworkResultMapperTest {
    @Test
    fun `success maps response data`() = runTest {
        val result = fetchAsDomainResult(
            call = { DataResponse(data = "value", errorCode = ApiCode.SUCCESS) },
            onSuccess = { it.data },
        )
        assertEquals(DomainResult.Success("value"), result)
    }

    @Test
    fun `login required is mapped to the unauthenticated domain code`() = runTest {
        val result = fetchAsDomainResult(
            call = { DataResponse<String>(errorCode = ApiCode.LOGIN_REQUIRED, errorMsg = "请登录") },
            onSuccess = { error("Must not process failed response") },
        )
        assertEquals(DomainResult.Failure(DomainResult.ERROR_UNAUTHENTICATED, "请登录"), result)
        assertTrue((result as DomainResult.Failure).isUnauthenticated)
    }

    @Test
    fun `other business codes are passed through unchanged`() = runTest {
        val result = fetchAsDomainResult(
            call = { DataResponse<String>(errorCode = "-1", errorMsg = "用户名已存在") },
            onSuccess = { error("Must not process failed response") },
        )
        assertEquals(DomainResult.Failure("-1", "用户名已存在"), result)
        assertFalse((result as DomainResult.Failure).isUnauthenticated)
    }

    @Test
    fun `blank code and blank message fall back to readable defaults`() = runTest {
        val result = fetchAsDomainResult(
            call = { DataResponse<String>() },
            onSuccess = { error("Must not process failed response") },
        )
        assertEquals(DomainResult.Failure(DomainResult.ERROR_UNKNOWN, "请求失败"), result)
    }

    @Test
    fun `network exception becomes domain failure`() = runTest {
        val result = fetchAsDomainResult<String, String?>(
            call = { throw IOException("offline") },
            onSuccess = { it.data },
        )
        assertEquals(DomainResult.Failure(DomainResult.ERROR_UNKNOWN, "offline"), result)
    }

    @Test
    fun `persistence exception becomes domain failure`() = runTest {
        val result = fetchAsDomainResult(
            call = { DataResponse(data = "value", errorCode = ApiCode.SUCCESS) },
            onSuccess = { throw IOException("write failed") },
        )
        assertEquals(DomainResult.Failure(DomainResult.ERROR_UNKNOWN, "数据处理失败，请重试"), result)
    }

    @Test
    fun `cancellation from request and persistence is rethrown`() = runTest {
        val cancellation = CancellationException("cancelled")
        for (cancelRequest in listOf(true, false)) {
            try {
                fetchAsDomainResult(
                    call = {
                        if (cancelRequest) throw cancellation
                        DataResponse(data = "value", errorCode = ApiCode.SUCCESS)
                    },
                    onSuccess = { throw cancellation },
                )
                fail("Cancellation must propagate")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }
    }

    @Test
    fun `getOrNull returns data on success and null on failure`() {
        assertEquals("v", DomainResult.Success("v").getOrNull())
        assertNull(DomainResult.Failure("-1", "x").getOrNull())
    }

    @Test
    fun `simple result success keeps the server message`() = runTest {
        val result = fetchAsSimpleResult("收藏失败") {
            HttpResponse(errorCode = ApiCode.SUCCESS, errorMsg = "ok")
        }
        assertEquals(SimpleResult(success = true, message = "ok"), result)
    }

    @Test
    fun `simple result business failure prefers the server message`() = runTest {
        val result = fetchAsSimpleResult("收藏失败") {
            HttpResponse(errorCode = ApiCode.LOGIN_REQUIRED, errorMsg = "请先登录")
        }
        assertEquals(SimpleResult(success = false, message = "请先登录"), result)
    }

    @Test
    fun `simple result business failure without message uses the fallback`() = runTest {
        val result = fetchAsSimpleResult("收藏失败") { HttpResponse(errorCode = "-1") }
        assertEquals(SimpleResult(success = false, message = "收藏失败"), result)
    }

    @Test
    fun `simple result treats a blank code as failure`() = runTest {
        val result = fetchAsSimpleResult("分享失败") { HttpResponse(errorMsg = "ok") }
        assertFalse(result.success)
    }

    @Test
    fun `simple result hides low level exception text`() = runTest {
        val result = fetchAsSimpleResult("分享失败") { throw IOException("SSL handshake failed: secret") }
        assertEquals(SimpleResult(success = false, message = "分享失败"), result)
    }

    @Test
    fun `simple result rethrows cancellation`() = runTest {
        val cancellation = CancellationException("cancelled")
        try {
            fetchAsSimpleResult("分享失败") { throw cancellation }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
