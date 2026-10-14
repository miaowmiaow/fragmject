package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.model.ApiCode
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.model.isSuccess
import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoinData
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.User
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 仓库层结果契约：业务码判断、失败消息和取消语义在各仓库保持一致。
 */
class RepositoryResultContractTest {

    private class FakeMyRemote : MyRemoteDataSource {
        var coin: suspend () -> DataResponse<Coin> = { DataResponse(Coin(userId = "1"), ApiCode.SUCCESS) }
        var action: suspend () -> HttpResponse = { HttpResponse(ApiCode.SUCCESS, "ok") }

        override suspend fun getUserCoin(): DataResponse<Coin> = coin()
        override suspend fun getMyCoinList(page: Int): DataResponse<MyCoinData> = error("unused")
        override suspend fun getMyShareList(page: Int): DataResponse<ShareArticle> = error("unused")
        override suspend fun shareArticle(title: String, link: String): HttpResponse = action()
        override suspend fun collectArticle(id: String): HttpResponse = action()
        override suspend fun uncollectArticle(id: String): HttpResponse = action()
    }

    private class FakeUserRemote : UserRemoteDataSource {
        var shares: suspend () -> DataResponse<ShareArticle> = { error("unset") }

        override suspend fun login(username: String, password: String): DataResponse<User> = error("unused")
        override suspend fun register(username: String, password: String, repassword: String): DataResponse<User> =
            error("unused")

        override suspend fun logout(): HttpResponse = error("unused")
        override suspend fun getUserShareArticles(userId: String, page: Int): DataResponse<ShareArticle> = shares()
    }

    private class FakeDownloader(var result: suspend () -> HttpResponse) : DownloadRemoteDataSource {
        override suspend fun download(
            url: String,
            savePath: String,
            fileName: String,
            headers: Map<String, String>,
        ): HttpResponse = result()
    }

    @Test
    fun `api codes keep the server wire values`() {
        // 这些值是与服务端的协议，改动会让所有仓库的成功/失败判断同时失真
        assertEquals("0", ApiCode.SUCCESS)
        assertEquals("-1", ApiCode.CLIENT_FAILURE)
        assertEquals("-1001", ApiCode.LOGIN_REQUIRED)
    }

    @Test
    fun `only the exact success code counts as success`() {
        assertTrue(HttpResponse(ApiCode.SUCCESS).isSuccess)
        listOf("", "-1", "-1001", "00", " 0", "0 ", "success").forEach {
            assertFalse("should be failure: '$it'", HttpResponse(it).isSuccess)
        }
        assertFalse(DataResponse<String>().isSuccess)
    }

    @Test
    fun `write operations map success and failure consistently`() = runBlocking {
        val remote = FakeMyRemote()
        val repo = MyRepositoryImpl(remote)

        assertEquals(SimpleResult(true, "ok"), repo.collectArticle("1"))

        remote.action = { HttpResponse(ApiCode.LOGIN_REQUIRED, "请先登录") }
        assertEquals(SimpleResult(false, "请先登录"), repo.shareArticle("t", "l"))

        remote.action = { HttpResponse("-1") }
        assertEquals(SimpleResult(false, "取消收藏失败"), repo.uncollectArticle("1"))

        remote.action = { throw IOException("offline") }
        assertEquals(SimpleResult(false, "收藏失败"), repo.collectArticle("1"))
        assertEquals(SimpleResult(false, "分享失败"), repo.shareArticle("t", "l"))
    }

    @Test
    fun `write operations rethrow the original cancellation`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val remote = FakeMyRemote().apply { action = { throw cancellation } }
        try {
            MyRepositoryImpl(remote).collectArticle("1")
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun `my coin only trusts a successful response`() = runBlocking {
        val remote = FakeMyRemote()
        val repo = MyRepositoryImpl(remote)

        assertEquals("1", repo.getUserCoin()?.userId)

        // 业务失败即使携带 data 也不能被当作有效积分（旧实现只读 .data，不看业务码）
        remote.coin = { DataResponse(Coin(userId = "ghost"), ApiCode.LOGIN_REQUIRED, "请登录") }
        assertNull(repo.getUserCoin())

        remote.coin = { throw IOException("offline") }
        assertNull(repo.getUserCoin())
    }

    @Test
    fun `my coin rethrows cancellation instead of degrading to null`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val remote = FakeMyRemote().apply { coin = { throw cancellation } }
        try {
            MyRepositoryImpl(remote).getUserCoin()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun `user center coin follows the same degrade and cancellation rules`() = runBlocking {
        val remote = FakeUserRemote()
        val repo = UserCenterRepositoryImpl(remote)

        remote.shares = {
            DataResponse(ShareArticle(coinInfo = Coin(userId = "9")), ApiCode.SUCCESS)
        }
        assertEquals("9", repo.getUserCoin("9")?.userId)

        remote.shares = {
            DataResponse(ShareArticle(coinInfo = Coin(userId = "ghost")), "-1", "失败")
        }
        assertNull(repo.getUserCoin("9"))

        remote.shares = { throw IOException("offline") }
        assertNull(repo.getUserCoin("9"))

        // 旧实现用 runCatching，会把取消吞成 null
        val cancellation = CancellationException("cancelled")
        remote.shares = { throw cancellation }
        try {
            repo.getUserCoin("9")
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun `download maps downloader responses through the shared entry`() = runBlocking {
        val downloader = FakeDownloader { HttpResponse(ApiCode.SUCCESS, "success") }
        val repo = DownloadRepositoryImpl(downloader)

        assertEquals(SimpleResult(true, "success"), repo.download("u", "p", "f"))

        downloader.result = { HttpResponse(ApiCode.CLIENT_FAILURE, "http 404 Not Found") }
        assertEquals(SimpleResult(false, "http 404 Not Found"), repo.download("u", "p", "f"))

        downloader.result = { HttpResponse(ApiCode.CLIENT_FAILURE) }
        assertEquals(SimpleResult(false, "下载失败"), repo.download("u", "p", "f"))

        downloader.result = { throw IOException("disk full") }
        assertEquals(SimpleResult(false, "下载失败"), repo.download("u", "p", "f"))
    }

    @Test
    fun `download rethrows cancellation so timeouts are not reported as failures`() = runBlocking {
        val cancellation = CancellationException("timeout")
        val repo = DownloadRepositoryImpl(FakeDownloader { throw cancellation })
        try {
            repo.download("u", "p", "f")
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
