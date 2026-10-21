package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.data.repository.paging.HomeFirstPagePrefetcher
import com.example.fragmject.core.data.contract.model.ApiCode
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.model.isSuccess
import com.example.fragmject.core.data.contract.local.HomeNavLocalDataSource
import com.example.fragmject.core.data.contract.local.HotKeyLocalDataSource
import com.example.fragmject.core.data.contract.local.ProjectTreeLocalDataSource
import com.example.fragmject.core.data.contract.local.SystemTreeLocalDataSource
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import com.example.fragmject.core.data.contract.remote.ProjectRemoteDataSource
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.CoinRankData
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.NavTab
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

    private class FakeArticleRemote : ArticleRemoteDataSource {
        var banners: suspend () -> DataResponse<List<Banner>> = { DataResponse(emptyList(), ApiCode.SUCCESS) }
        var top: suspend () -> DataResponse<List<Article>> = { DataResponse(emptyList(), ApiCode.SUCCESS) }

        override suspend fun getArticleList(page: Int): DataResponse<ArticleData> = error("unused")
        override suspend fun getArticleListByCid(page: Int, cid: String): DataResponse<ArticleData> = error("unused")
        override suspend fun searchArticles(key: String, page: Int): DataResponse<ArticleData> = error("unused")
        override suspend fun getCollectList(page: Int): DataResponse<ArticleData> = error("unused")
        override suspend fun fetchBannerList(): DataResponse<List<Banner>> = banners()
        override suspend fun fetchArticleTop(): DataResponse<List<Article>> = top()
    }

    @Test
    fun `home header maps success business failure and io failure`() = runBlocking {
        val remote = FakeArticleRemote()
        val repo = HomeRepositoryImpl(remote, HomeFirstPagePrefetcher(remote, AppCoroutineScope()))

        remote.banners = { DataResponse(listOf(Banner(id = "b1")), ApiCode.SUCCESS) }
        val success = repo.fetchHomeBanners()
        assertTrue(success is DomainResult.Success)
        assertEquals("b1", (success as DomainResult.Success).data.single().id)

        // 业务失败要保留服务端文案，否则 UI 只能显示兜底提示
        remote.banners = { DataResponse(null, "-1", "服务开小差") }
        val businessFailure = repo.fetchHomeBanners()
        assertTrue(businessFailure is DomainResult.Failure)
        assertEquals("服务开小差", (businessFailure as DomainResult.Failure).message)

        // 网络异常走 ERROR_UNKNOWN，并把异常信息带出去便于排查
        remote.banners = { throw IOException("offline") }
        val ioFailure = repo.fetchHomeBanners()
        assertEquals(DomainResult.ERROR_UNKNOWN, (ioFailure as DomainResult.Failure).code)
        assertEquals("offline", ioFailure.message)
    }

    @Test
    fun `home header rethrows cancellation instead of degrading to failure`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val remote = FakeArticleRemote().apply { top = { throw cancellation } }
        try {
            HomeRepositoryImpl(remote, HomeFirstPagePrefetcher(remote, AppCoroutineScope())).fetchTopArticles()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    private class FakeHotKeyLocal : HotKeyLocalDataSource {
        var observe: () -> Flow<List<HotKey>> = { emptyFlow() }
        val saved = mutableListOf<List<HotKey>>()

        override fun observeHotKey(): Flow<List<HotKey>> = observe()
        override suspend fun saveHotKey(items: List<HotKey>) {
            saved += items
        }
    }

    private class FakeProjectTreeLocal : ProjectTreeLocalDataSource {
        var observe: () -> Flow<List<ProjectTree>> = { emptyFlow() }
        val saved = mutableListOf<List<ProjectTree>>()

        override fun observeProjectTree(): Flow<List<ProjectTree>> = observe()
        override suspend fun saveProjectTree(items: List<ProjectTree>) {
            saved += items
        }
    }

    private class FakeCommonRemote : CommonRemoteDataSource {
        var hotKey: suspend () -> DataResponse<List<HotKey>> = { DataResponse(emptyList(), ApiCode.SUCCESS) }
        var navigation: suspend () -> DataResponse<MutableList<NavTab>> = { DataResponse(mutableListOf(), ApiCode.SUCCESS) }
        var systemTree: suspend () -> DataResponse<MutableList<Tree>> = { DataResponse(mutableListOf(), ApiCode.SUCCESS) }

        override suspend fun getCoinRank(page: Int): DataResponse<CoinRankData> = error("unused")
        override suspend fun fetchNavigation(): DataResponse<MutableList<NavTab>> = navigation()
        override suspend fun fetchSystemTree(): DataResponse<MutableList<Tree>> = systemTree()
        override suspend fun fetchHotKey(): DataResponse<List<HotKey>> = hotKey()
    }

    private class FakeHomeNavLocal : HomeNavLocalDataSource {
        var observe: () -> Flow<List<NavTab>> = { emptyFlow() }
        val saved = mutableListOf<List<NavTab>>()

        override fun observeNavigation(): Flow<List<NavTab>> = observe()
        override suspend fun saveNavigation(items: List<NavTab>) {
            saved += items
        }
    }

    private class FakeSystemTreeLocal : SystemTreeLocalDataSource {
        var observe: () -> Flow<List<Tree>> = { emptyFlow() }
        val saved = mutableListOf<List<Tree>>()

        override fun observeSystemTree(): Flow<List<Tree>> = observe()
        override suspend fun saveSystemTree(items: List<Tree>) {
            saved += items
        }
    }

    private class FakeProjectRemote : ProjectRemoteDataSource {
        var tree: suspend () -> DataResponse<List<ProjectTree>> = { DataResponse(emptyList(), ApiCode.SUCCESS) }

        override suspend fun getProjectList(page: Int, cid: String): DataResponse<ArticleData> = error("unused")
        override suspend fun fetchProjectTree(): DataResponse<List<ProjectTree>> = tree()
    }

    @Test
    fun `search hot key refresh caches the payload only on success`() = runBlocking {
        val local = FakeHotKeyLocal()
        val common = FakeCommonRemote()
        val repo = SearchRepositoryImpl(local, FakeArticleRemote(), common)

        common.hotKey = { DataResponse(listOf(HotKey(id = "h1", name = "Kotlin")), ApiCode.SUCCESS) }
        assertTrue(repo.refreshHotKey() is DomainResult.Success)
        assertEquals(listOf("h1"), local.saved.single().map { it.id })

        // 业务失败即使携带 data 也不能污染本地缓存
        local.saved.clear()
        common.hotKey = { DataResponse(listOf(HotKey(id = "ghost")), "-1", "服务开小差") }
        val businessFailure = repo.refreshHotKey()
        assertTrue(businessFailure is DomainResult.Failure)
        assertEquals("服务开小差", (businessFailure as DomainResult.Failure).message)
        assertTrue(local.saved.isEmpty())

        common.hotKey = { throw IOException("offline") }
        val ioFailure = repo.refreshHotKey()
        assertEquals(DomainResult.ERROR_UNKNOWN, (ioFailure as DomainResult.Failure).code)
        assertEquals("offline", ioFailure.message)
        assertTrue(local.saved.isEmpty())

        // 成功但无 payload 时视为刷新完成，不写入空缓存
        common.hotKey = { DataResponse(null, ApiCode.SUCCESS) }
        assertTrue(repo.refreshHotKey() is DomainResult.Success)
        assertTrue(local.saved.isEmpty())
    }

    @Test
    fun `search hot key refresh rethrows cancellation instead of degrading to failure`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val common = FakeCommonRemote().apply { hotKey = { throw cancellation } }
        try {
            SearchRepositoryImpl(FakeHotKeyLocal(), FakeArticleRemote(), common).refreshHotKey()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun `project tree refresh caches the payload only on success`() = runBlocking {
        val local = FakeProjectTreeLocal()
        val remote = FakeProjectRemote()
        val repo = ProjectRepositoryImpl(local, remote)

        remote.tree = { DataResponse(listOf(ProjectTree(id = "p1", name = "项目")), ApiCode.SUCCESS) }
        assertTrue(repo.refreshProjectTree() is DomainResult.Success)
        assertEquals(listOf("p1"), local.saved.single().map { it.id })

        local.saved.clear()
        remote.tree = { DataResponse(listOf(ProjectTree(id = "ghost")), "-1", "服务开小差") }
        val businessFailure = repo.refreshProjectTree()
        assertEquals("服务开小差", (businessFailure as DomainResult.Failure).message)

        remote.tree = { DataResponse(listOf(ProjectTree(id = "ghost")), ApiCode.LOGIN_REQUIRED, "请先登录") }
        assertEquals(DomainResult.ERROR_UNAUTHENTICATED, (repo.refreshProjectTree() as DomainResult.Failure).code)

        remote.tree = { throw IOException("offline") }
        val ioFailure = repo.refreshProjectTree()
        assertEquals(DomainResult.ERROR_UNKNOWN, (ioFailure as DomainResult.Failure).code)
        assertTrue(local.saved.isEmpty())
    }

    @Test
    fun `project tree refresh rethrows cancellation instead of degrading to failure`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val remote = FakeProjectRemote().apply { tree = { throw cancellation } }
        try {
            ProjectRepositoryImpl(FakeProjectTreeLocal(), remote).refreshProjectTree()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun `home nav refresh caches the payload only on success`() = runBlocking {
        val local = FakeHomeNavLocal()
        val common = FakeCommonRemote()
        val repo = HomeNavRepositoryImpl(local, common)

        common.navigation = { DataResponse(mutableListOf(NavTab(cid = "n1", name = "导航")), ApiCode.SUCCESS) }
        assertTrue(repo.refreshNavigation() is DomainResult.Success)
        assertEquals(listOf("n1"), local.saved.single().map { it.cid })

        local.saved.clear()
        common.navigation = { DataResponse(mutableListOf(NavTab(cid = "ghost")), "-1", "服务开小差") }
        assertEquals("服务开小差", (repo.refreshNavigation() as DomainResult.Failure).message)

        common.navigation = { throw IOException("offline") }
        val ioFailure = repo.refreshNavigation()
        assertEquals(DomainResult.ERROR_UNKNOWN, (ioFailure as DomainResult.Failure).code)
        assertEquals("offline", ioFailure.message)
        assertTrue(local.saved.isEmpty())

        // 成功但无 payload 时不写入空缓存
        common.navigation = { DataResponse(null, ApiCode.SUCCESS) }
        assertTrue(repo.refreshNavigation() is DomainResult.Success)
        assertTrue(local.saved.isEmpty())
    }

    @Test
    fun `system tree refresh caches the payload only on success`() = runBlocking {
        val local = FakeSystemTreeLocal()
        val common = FakeCommonRemote()
        val repo = SystemRepositoryImpl(FakeArticleRemote(), local, common)

        common.systemTree = { DataResponse(mutableListOf(Tree(id = "t1", name = "体系")), ApiCode.SUCCESS) }
        assertTrue(repo.refreshSystemTree() is DomainResult.Success)
        assertEquals(listOf("t1"), local.saved.single().map { it.id })

        local.saved.clear()
        common.systemTree = { DataResponse(mutableListOf(Tree(id = "ghost")), "-1", "服务开小差") }
        assertEquals("服务开小差", (repo.refreshSystemTree() as DomainResult.Failure).message)

        common.systemTree = { DataResponse(mutableListOf(Tree(id = "ghost")), ApiCode.LOGIN_REQUIRED, "请先登录") }
        assertEquals(DomainResult.ERROR_UNAUTHENTICATED, (repo.refreshSystemTree() as DomainResult.Failure).code)

        common.systemTree = { throw IOException("offline") }
        val ioFailure = repo.refreshSystemTree()
        assertEquals(DomainResult.ERROR_UNKNOWN, (ioFailure as DomainResult.Failure).code)
        assertTrue(local.saved.isEmpty())
    }

    @Test
    fun `home nav and system tree refreshes rethrow cancellation`() = runBlocking {
        val cancellation = CancellationException("cancelled")

        val navRemote = FakeCommonRemote().apply { navigation = { throw cancellation } }
        try {
            HomeNavRepositoryImpl(FakeHomeNavLocal(), navRemote).refreshNavigation()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }

        val treeRemote = FakeCommonRemote().apply { systemTree = { throw cancellation } }
        try {
            SystemRepositoryImpl(FakeArticleRemote(), FakeSystemTreeLocal(), treeRemote).refreshSystemTree()
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
