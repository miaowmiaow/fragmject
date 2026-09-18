package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.FakeArticleDao
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.network.datasource.ArticleDataSource
import com.example.fragmject.core.network.http.DataResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [OfflineFirstArticleRepository] 的 offline-first 逻辑测试。
 *
 * 覆盖「page 0 进 Room、page 1+ 仅网络」的分治契约，
 * 以及 refresh 失败时不污染缓存的兜底行为。
 */
class OfflineFirstArticleRepositoryTest {

    private val page0Key = OfflineFirstArticleRepository.CACHE_KEY_PAGE_0

    @Test
    fun `refreshHome success - writes page0 and returns pageCount`() = runBlocking {
        val dao = FakeArticleDao()
        val dataSource = HomeFakeArticleDataSource(
            articleList = { page ->
                DataResponse(
                    data = ArticleData(
                        pageCount = "3",
                        datas = listOf(
                            Article(id = "1", title = "首页1"),
                            Article(id = "2", title = "首页2"),
                        ),
                    ),
                    errorCode = "0",
                )
            }
        )
        val repo = OfflineFirstArticleRepository(dao, dataSource)

        val result = repo.refreshHome()

        assertTrue(result is DomainResult.Success)
        assertEquals(3, (result as DomainResult.Success).data)
        assertEquals(2, dao.count(page0Key))
    }

    @Test
    fun `refreshHome failure - does not pollute cache`() = runBlocking {
        val dao = FakeArticleDao()
        val dataSource = HomeFakeArticleDataSource(
            articleList = { _ -> DataResponse(errorCode = "-1", errorMsg = "boom") }
        )
        val repo = OfflineFirstArticleRepository(dao, dataSource)

        val result = repo.refreshHome()

        assertTrue(result is DomainResult.Failure)
        assertEquals("-1", (result as DomainResult.Failure).code)
        assertEquals(0, dao.count(page0Key))
    }

    @Test
    fun `loadNextPage - returns articles without writing room`() = runBlocking {
        val dao = FakeArticleDao()
        val dataSource = HomeFakeArticleDataSource(
            articleList = { page ->
                DataResponse(
                    data = ArticleData(
                        pageCount = "3",
                        datas = listOf(Article(id = "9", title = "第2页")),
                    ),
                    errorCode = "0",
                )
            }
        )
        val repo = OfflineFirstArticleRepository(dao, dataSource)

        val result = repo.loadNextPage(1)

        assertTrue(result is DomainResult.Success)
        val pageData = (result as DomainResult.Success).data
        assertEquals(1, pageData.articles.size)
        assertEquals("第2页", pageData.articles[0].title)
        assertEquals(3, pageData.pageCount)
        // 后续页不写 Room
        assertEquals(0, dao.count(page0Key))
    }
}

private class HomeFakeArticleDataSource(
    private val articleList: (Int) -> DataResponse<ArticleData> = { DataResponse() },
) : ArticleDataSource {
    override suspend fun getArticleList(page: Int): DataResponse<ArticleData> = articleList(page)
    override suspend fun getArticleListByCid(page: Int, cid: String): DataResponse<ArticleData> = DataResponse()
    override suspend fun searchArticles(key: String, page: Int): DataResponse<ArticleData> = DataResponse()
    override suspend fun getCollectList(page: Int): DataResponse<ArticleData> = DataResponse()
    override suspend fun fetchBannerList(): DataResponse<List<Banner>> = DataResponse()
    override suspend fun fetchArticleTop(): DataResponse<List<Article>> = DataResponse()
}
