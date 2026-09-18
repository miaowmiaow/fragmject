package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.FakeArticleDao
import com.example.fragmject.core.database.model.ArticleEntity
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.network.datasource.ArticleDataSource
import com.example.fragmject.core.network.http.DataResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [OfflineFirstMyCollectRepository] 的 offline-first 逻辑测试。
 *
 * 不依赖真实 Room / Retrofit：以内存 [FakeArticleDao] 与 [FakeArticleDataSource]
 * 替换 DAO 与网络数据源，验证「page 0 进缓存、后续页走网络」的分治契约。
 * 这正是 Phase 1 将 DAO 改为接口注入后解锁的测试能力。
 */
class OfflineFirstMyCollectRepositoryTest {

    private val cacheKey = OfflineFirstMyCollectRepository.CACHE_KEY_PAGE_0

    // ===== 成功路径：刷新写缓存并返回页数 =====

    @Test
    fun `refresh success - writes page0 and returns pageCount`() = runBlocking {
        val dao = FakeArticleDao()
        val dataSource = FakeArticleDataSource(
            collectResult = { page ->
                DataResponse(
                    data = ArticleData(
                        pageCount = "3",
                        datas = listOf(
                            Article(id = "1", title = "收藏1"),
                            Article(id = "2", title = "收藏2"),
                        ),
                    ),
                    errorCode = "0",
                )
            }
        )
        val repo = OfflineFirstMyCollectRepository(dao, dataSource)

        val result = repo.refreshMyCollect()

        assertTrue(result is DomainResult.Success)
        assertEquals(3, (result as DomainResult.Success).data)
        assertEquals(2, dao.count(cacheKey))
    }

    // ===== 失败路径：不写缓存 =====

    @Test
    fun `refresh failure - does not write cache`() = runBlocking {
        val dao = FakeArticleDao()
        val dataSource = FakeArticleDataSource(
            collectResult = { _ ->
                DataResponse(errorCode = "-1", errorMsg = "boom")
            }
        )
        val repo = OfflineFirstMyCollectRepository(dao, dataSource)

        val result = repo.refreshMyCollect()

        assertTrue(result is DomainResult.Failure)
        assertEquals("-1", (result as DomainResult.Failure).code)
        assertEquals(0, dao.count(cacheKey))
    }

    // ===== 后续页：仅网络，不写缓存 =====

    @Test
    fun `loadNextPage success - returns articles without writing cache`() = runBlocking {
        val dao = FakeArticleDao()
        val dataSource = FakeArticleDataSource(
            collectResult = { page ->
                DataResponse(
                    data = ArticleData(
                        pageCount = "3",
                        datas = listOf(Article(id = "9", title = "第2页")),
                    ),
                    errorCode = "0",
                )
            }
        )
        val repo = OfflineFirstMyCollectRepository(dao, dataSource)

        val result = repo.loadMyCollectNextPage(1)

        assertTrue(result is DomainResult.Success)
        val pageData = (result as DomainResult.Success).data
        assertEquals(1, pageData.articles.size)
        assertEquals("第2页", pageData.articles[0].title)
        assertEquals(3, pageData.pageCount)
        // 后续页不写 Room
        assertEquals(0, dao.count(cacheKey))
    }

    // ===== observe：从 DAO 映射为领域模型 =====

    @Test
    fun `observeMyCollect - emits domain articles from dao`() = runBlocking {
        val dao = FakeArticleDao()
        dao.insertAll(
            listOf(
                ArticleEntity(articleId = "1", cacheKey = cacheKey, sortOrder = 0, title = "标题1"),
                ArticleEntity(articleId = "2", cacheKey = cacheKey, sortOrder = 1, title = "标题2"),
            )
        )
        val repo = OfflineFirstMyCollectRepository(dao, FakeArticleDataSource { DataResponse() })

        val articles = repo.observeMyCollect().first()

        assertEquals(2, articles.size)
        assertEquals("1", articles[0].id)
        assertEquals("标题2", articles[1].title)
    }
}

/**
 * 可编程的 [ArticleDataSource]：仅收藏接口按 [collectResult] 返回，其余返回空响应。
 */
private class FakeArticleDataSource(
    private val collectResult: suspend (Int) -> DataResponse<ArticleData>,
) : ArticleDataSource {
    override suspend fun getArticleList(page: Int): DataResponse<ArticleData> = DataResponse()
    override suspend fun getArticleListByCid(page: Int, cid: String): DataResponse<ArticleData> = DataResponse()
    override suspend fun searchArticles(key: String, page: Int): DataResponse<ArticleData> = DataResponse()
    override suspend fun getCollectList(page: Int): DataResponse<ArticleData> = collectResult(page)
    override suspend fun fetchBannerList(): DataResponse<List<Banner>> = DataResponse()
    override suspend fun fetchArticleTop(): DataResponse<List<Article>> = DataResponse()
}
