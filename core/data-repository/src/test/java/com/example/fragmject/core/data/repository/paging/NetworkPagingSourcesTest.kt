package com.example.fragmject.core.data.repository.paging

import androidx.paging.PagingSource
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource
import com.example.fragmject.core.data.contract.remote.ProjectRemoteDataSource
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.CoinRankData
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.model.MyCoinData
import com.example.fragmject.core.model.NavTab
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.model.User
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NetworkPagingSourcesTest {

    @Test
    fun `all sources preserve initial pages and request arguments`() = runBlocking {
        val remote = FakePagingRemote()
        for (case in sourceCases(remote)) {
            val page = requirePage(case.name, load(case.create(remote)))

            assertEquals(case.name, case.items, page.data)
            assertNull(case.name, page.prevKey)
            assertEquals(case.name, case.startPage + 1, page.nextKey)
            assertEquals(case.name, case.request, remote.lastRequest)
        }
    }

    @Test
    fun `all sources preserve explicit page keys`() = runBlocking {
        val remote = FakePagingRemote()
        for (case in sourceCases(remote)) {
            val requestedPage = case.startPage + 2
            val page = requirePage(case.name, load(case.create(remote), requestedPage))

            assertEquals(case.name, requestedPage - 1, page.prevKey)
            assertEquals(case.name, requestedPage + 1, page.nextKey)
            assertEquals(case.name, case.request.copy(page = requestedPage), remote.lastRequest)
        }
    }

    @Test
    fun `all sources stop at nonempty server final page`() = runBlocking {
        val remote = FakePagingRemote().apply { over = true }
        for (case in sourceCases(remote)) {
            val page = requirePage(case.name, load(case.create(remote)))

            assertEquals(case.name, case.items, page.data)
            assertNull(case.name, page.nextKey)
        }
    }

    @Test
    fun `all sources still stop at a real empty page`() = runBlocking {
        val remote = FakePagingRemote().apply { emptyItems = true }
        for (case in sourceCases(remote)) {
            val page = requirePage(case.name, load(case.create(remote)))

            assertTrue(case.name, page.data.isEmpty())
            assertNull(case.name, page.nextKey)
        }
    }

    @Test
    fun `all sources reject business failures even with data`() = runBlocking {
        val remote = FakePagingRemote()
        for (code in listOf("-1", "-1001", "")) {
            remote.errorCode = code
            remote.errorMsg = "分页请求被拒绝"
            for (case in sourceCases(remote)) {
                val failure = requireFailure(case.name, load(case.create(remote)))

                assertTrue(case.name, failure is PagingResponseException)
                assertEquals(case.name, code, (failure as PagingResponseException).errorCode)
                assertTrue(case.name, failure.message.orEmpty().contains(remote.errorMsg))
                assertTrue(case.name, failure.message.orEmpty().contains(code.ifBlank { "缺失" }))
            }
        }
    }

    @Test
    fun `business failure has a readable fallback and precedes missing data`() = runBlocking {
        val remote = FakePagingRemote().apply {
            errorCode = "-1"
            nullData = true
        }
        for (case in sourceCases(remote)) {
            val failure = requireFailure(case.name, load(case.create(remote)))

            assertTrue(case.name, failure is PagingResponseException)
            assertEquals(case.name, "-1", (failure as PagingResponseException).errorCode)
            assertTrue(case.name, failure.message.orEmpty().contains("分页请求失败"))
        }
    }

    @Test
    fun `all sources reject success with null data`() = runBlocking {
        val remote = FakePagingRemote().apply { nullData = true }
        for (case in sourceCases(remote)) {
            val failure = requireFailure(case.name, load(case.create(remote)))

            assertTrue(case.name, failure is PagingResponseException)
            assertEquals(case.name, "分页响应缺少必需字段：data", failure.message)
        }
    }

    @Test
    fun `share sources reject missing nested page data`() = runBlocking {
        val remote = FakePagingRemote().apply { missingShareArticles = true }
        for (case in sourceCases(remote).filter { it.request.endpoint.contains("share") }) {
            val failure = requireFailure(case.name, load(case.create(remote)))

            assertTrue(case.name, failure is PagingResponseException)
            assertEquals(case.name, "分页响应缺少必需字段：data.shareArticles", failure.message)
        }
    }

    @Test
    fun `all sources reject missing item lists instead of inventing empty pages`() = runBlocking {
        val remote = FakePagingRemote().apply { missingItems = true }
        for (case in sourceCases(remote)) {
            val failure = requireFailure(case.name, load(case.create(remote)))
            val field = if (case.request.endpoint.contains("share")) {
                "data.shareArticles.datas"
            } else {
                "data.datas"
            }

            assertTrue(case.name, failure is PagingResponseException)
            assertEquals(case.name, "分页响应缺少必需字段：$field", failure.message)
        }
    }

    @Test
    fun `all sources preserve request exceptions as load errors`() = runBlocking {
        val expected = IOException("请求失败")
        val remote = FakePagingRemote().apply { requestFailure = expected }
        for (case in sourceCases(remote)) {
            assertSame(case.name, expected, requireFailure(case.name, load(case.create(remote))))
        }
    }

    @Test
    fun `all sources rethrow the original cancellation`() = runBlocking {
        val expected = CancellationException("取消分页加载")
        val remote = FakePagingRemote().apply { requestFailure = expected }
        for (case in sourceCases(remote)) {
            try {
                load(case.create(remote))
                fail("${case.name}: cancellation must not become a load result")
            } catch (actual: CancellationException) {
                assertSame(case.name, expected, actual)
            }
        }
    }

    private suspend fun load(source: PagingSource<Int, *>, key: Int? = null) =
        source.load(
            PagingSource.LoadParams.Refresh(
                key = key,
                loadSize = 20,
                placeholdersEnabled = false,
            )
        )

    private fun requirePage(
        name: String,
        result: PagingSource.LoadResult<Int, *>,
    ): PagingSource.LoadResult.Page<Int, *> {
        assertTrue(name, result is PagingSource.LoadResult.Page<*, *>)
        return result as PagingSource.LoadResult.Page<Int, *>
    }

    private fun requireFailure(name: String, result: PagingSource.LoadResult<Int, *>): Throwable {
        assertTrue(name, result is PagingSource.LoadResult.Error<*, *>)
        return (result as PagingSource.LoadResult.Error<Int, *>).throwable
    }

    private fun sourceCases(remote: FakePagingRemote): List<SourceCase> = listOf(
        SourceCase("home", 0, Request("home", 0), listOf(remote.article)) { HomePagingSource(it) },
        SourceCase("system", 0, Request("system", 0, "42"), listOf(remote.article)) {
            SystemPagingSource("42", it)
        },
        SourceCase("search", 0, Request("search", 0, "kotlin"), listOf(remote.article)) {
            SearchPagingSource("kotlin", it)
        },
        SourceCase("collect", 0, Request("collect", 0), listOf(remote.article)) {
            MyCollectPagingSource(it)
        },
        SourceCase("project", 1, Request("project", 1, "43"), listOf(remote.article)) {
            ProjectPagingSource("43", it)
        },
        SourceCase("my share", 0, Request("my share", 0), listOf(remote.article)) {
            MySharePagingSource(it)
        },
        SourceCase("user share", 1, Request("user share", 1, "7"), listOf(remote.article)) {
            UserSharePagingSource("7", it)
        },
        SourceCase("my coin", 1, Request("my coin", 1), listOf(remote.myCoin)) {
            MyCoinPagingSource(it)
        },
        SourceCase("coin rank", 1, Request("coin rank", 1), listOf(remote.coin)) {
            CoinRankPagingSource(it)
        },
    )
}

private data class SourceCase(
    val name: String,
    val startPage: Int,
    val request: Request,
    val items: List<Any>,
    val create: (FakePagingRemote) -> PagingSource<Int, *>,
)

private data class Request(val endpoint: String, val page: Int, val argument: String? = null)

private class FakePagingRemote : ArticleRemoteDataSource, CommonRemoteDataSource,
    MyRemoteDataSource, ProjectRemoteDataSource, UserRemoteDataSource {

    val article = Article(id = "1", title = "测试文章")
    val myCoin = MyCoin(id = "2")
    val coin = Coin(userId = "3")
    var errorCode = "0"
    var errorMsg = ""
    var over = false
    var emptyItems = false
    var missingItems = false
    var nullData = false
    var missingShareArticles = false
    var requestFailure: Exception? = null
    var lastRequest: Request? = null

    private fun <T> respond(request: Request, data: T): DataResponse<T> {
        lastRequest = request
        requestFailure?.let { throw it }
        return DataResponse(
            data = if (nullData) null else data,
            errorCode = errorCode,
            errorMsg = errorMsg,
        )
    }

    private fun <T> items(item: T): List<T>? = when {
        missingItems -> null
        emptyItems -> emptyList()
        else -> listOf(item)
    }

    private fun articleData() = ArticleData(datas = items(article), over = over)

    private fun shareData() = ShareArticle(
        shareArticles = if (missingShareArticles) null else articleData(),
    )

    override suspend fun getArticleList(page: Int): DataResponse<ArticleData> =
        respond(Request("home", page), articleData())

    override suspend fun getArticleListByCid(page: Int, cid: String): DataResponse<ArticleData> =
        respond(Request("system", page, cid), articleData())

    override suspend fun searchArticles(key: String, page: Int): DataResponse<ArticleData> =
        respond(Request("search", page, key), articleData())

    override suspend fun getCollectList(page: Int): DataResponse<ArticleData> =
        respond(Request("collect", page), articleData())

    override suspend fun getProjectList(page: Int, cid: String): DataResponse<ArticleData> =
        respond(Request("project", page, cid), articleData())

    override suspend fun getMyShareList(page: Int): DataResponse<ShareArticle> =
        respond(Request("my share", page), shareData())

    override suspend fun getUserShareArticles(userId: String, page: Int): DataResponse<ShareArticle> =
        respond(Request("user share", page, userId), shareData())

    override suspend fun getMyCoinList(page: Int): DataResponse<MyCoinData> =
        respond(Request("my coin", page), MyCoinData(datas = items(myCoin), over = over))

    override suspend fun getCoinRank(page: Int): DataResponse<CoinRankData> =
        respond(Request("coin rank", page), CoinRankData(datas = items(coin), over = over))

    override suspend fun fetchBannerList(): DataResponse<List<Banner>> = error("Unexpected request")
    override suspend fun fetchArticleTop(): DataResponse<List<Article>> = error("Unexpected request")
    override suspend fun fetchNavigation(): DataResponse<MutableList<NavTab>> = error("Unexpected request")
    override suspend fun fetchSystemTree(): DataResponse<MutableList<Tree>> = error("Unexpected request")
    override suspend fun fetchHotKey(): DataResponse<List<HotKey>> = error("Unexpected request")
    override suspend fun getUserCoin(): DataResponse<Coin> = error("Unexpected request")
    override suspend fun shareArticle(title: String, link: String): HttpResponse = error("Unexpected request")
    override suspend fun collectArticle(id: String): HttpResponse = error("Unexpected request")
    override suspend fun uncollectArticle(id: String): HttpResponse = error("Unexpected request")
    override suspend fun fetchProjectTree(): DataResponse<List<ProjectTree>> = error("Unexpected request")
    override suspend fun login(username: String, password: String): DataResponse<User> = error("Unexpected request")
    override suspend fun register(username: String, password: String, repassword: String): DataResponse<User> =
        error("Unexpected request")
    override suspend fun logout(): HttpResponse = error("Unexpected request")
}
