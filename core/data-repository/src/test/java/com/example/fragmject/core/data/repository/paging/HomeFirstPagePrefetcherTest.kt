package com.example.fragmject.core.data.repository.paging

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * [HomeFirstPagePrefetcher] 语义测试。
 *
 * 覆盖三条对首屏正确性有直接影响的约定：
 * 1. `warmUp()` 幂等 —— 重复调用不能重复发请求（否则每个入口都预热就会翻倍）；
 * 2. 预取结果一次性消费 —— 下拉刷新 / 重建 PagingSource 必须重新取数，不能复用旧页；
 * 3. 预取失败回退 —— 失败语义与「没有预取」完全一致，不能让首屏直接进错误态。
 */
class HomeFirstPagePrefetcherTest {

    @Test
    fun `warm up is idempotent and the first await consumes the prefetched page`() = runBlocking {
        val remote = FakeArticleRemote()
        val prefetcher = HomeFirstPagePrefetcher(remote, AppCoroutineScope())

        prefetcher.warmUp()
        prefetcher.warmUp()

        val page = withTimeout(TIMEOUT_MS.milliseconds) { prefetcher.awaitFirstPage() }

        assertEquals(listOf(PREFETCHED_TITLE), page.items.map { it.title })
        assertEquals("重复 warmUp 不应重复发请求", 1, remote.callCount)
    }

    @Test
    fun `prefetch result is consumed only once`() = runBlocking {
        val remote = FakeArticleRemote()
        val prefetcher = HomeFirstPagePrefetcher(remote, AppCoroutineScope())
        prefetcher.warmUp()

        withTimeout(TIMEOUT_MS.milliseconds) { prefetcher.awaitFirstPage() }
        val second = withTimeout(TIMEOUT_MS.milliseconds) { prefetcher.awaitFirstPage() }

        assertEquals(listOf(PREFETCHED_TITLE), second.items.map { it.title })
        assertEquals("第二次消费必须重新请求", 2, remote.callCount)
    }

    @Test
    fun `await without warm up performs a normal request`() = runBlocking {
        val remote = FakeArticleRemote()
        val prefetcher = HomeFirstPagePrefetcher(remote, AppCoroutineScope())

        val page = withTimeout(TIMEOUT_MS.milliseconds) { prefetcher.awaitFirstPage() }

        assertEquals(listOf(PREFETCHED_TITLE), page.items.map { it.title })
        assertEquals(1, remote.callCount)
    }

    @Test
    fun `prefetch failure falls back to a fresh request instead of surfacing the error`() = runBlocking {
        val remote = FakeArticleRemote().apply { failuresBeforeSuccess = 1 }
        val prefetcher = HomeFirstPagePrefetcher(remote, AppCoroutineScope())

        prefetcher.warmUp()
        val page = withTimeout(TIMEOUT_MS.milliseconds) { prefetcher.awaitFirstPage() }

        assertEquals(listOf(PREFETCHED_TITLE), page.items.map { it.title })
        assertEquals("预取失败后应重新请求一次", 2, remote.callCount)
    }

    @Test
    fun `server failure is surfaced when there is no prefetch to fall back to`() = runBlocking {
        val remote = FakeArticleRemote().apply { errorCode = "-1" }
        val prefetcher = HomeFirstPagePrefetcher(remote, AppCoroutineScope())

        var failure: Throwable? = null
        try {
            withTimeout(TIMEOUT_MS.milliseconds) { prefetcher.awaitFirstPage() }
        } catch (e: Exception) {
            failure = e
        }

        // 业务失败必须冒泡成异常，由 BasePagingSource 转成 LoadResult.Error 展示错误态。
        assertEquals(PagingResponseException::class.java, failure?.javaClass)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val PREFETCHED_TITLE = "预取"
    }
}

/** 最小化的文章远端：只实现首页分页，其余方法明确失败以暴露误用。 */
private class FakeArticleRemote : ArticleRemoteDataSource {

    var callCount = 0

    /** 前 N 次请求抛 IO 异常，用于验证预取失败后的回退。 */
    var failuresBeforeSuccess = 0

    var errorCode = "0"

    override suspend fun getArticleList(page: Int): DataResponse<ArticleData> {
        callCount++
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            throw IOException("offline")
        }
        return DataResponse(
            data = ArticleData(
                datas = listOf(Article(id = "1", title = "预取")),
                over = false,
            ),
            errorCode = errorCode,
            errorMsg = "",
        )
    }

    override suspend fun getArticleListByCid(page: Int, cid: String): DataResponse<ArticleData> =
        error("unused")

    override suspend fun searchArticles(key: String, page: Int): DataResponse<ArticleData> =
        error("unused")

    override suspend fun getCollectList(page: Int): DataResponse<ArticleData> = error("unused")

    override suspend fun fetchBannerList(): DataResponse<List<Banner>> = error("unused")

    override suspend fun fetchArticleTop(): DataResponse<List<Article>> = error("unused")
}
