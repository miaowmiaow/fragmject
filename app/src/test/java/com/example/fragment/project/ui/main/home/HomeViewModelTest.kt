package com.example.fragment.project.ui.main.home

import com.example.fragment.project.data.ArticleData
import com.example.fragment.project.data.ArticleList
import com.example.fragment.project.data.BannerList
import com.example.fragment.project.data.TopArticle
import com.example.fragment.project.data.repository.ArticleRepository
import com.example.fragment.project.data.repository.CachedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * [HomeViewModel] 与 Repository 解耦后的示范单测。
 *
 * 使用方式：
 * 1. 通过构造参数注入 [FakeArticleRepository]，无需真实网络；
 * 2. 用 [StandardTestDispatcher] 接管 `Dispatchers.Main`，让 `viewModelScope.launch` 在测试线程上跑；
 * 3. 通过 [advanceUntilIdle] 等价于"让所有挂起协程跑完"。
 *
 * Repository 已升级到 SWR Flow 模型：首屏的 banner / top / list 通过
 * `*Flow()` 接入"先缓存后网络"的两段式发射；本测试仅模拟"无缓存、网络一次发射"
 * 的最简路径，对应阶段栅栏中"缓存阶段无命中 → 网络阶段统一发射一次"的链路。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers getHome and exposes loaded state`() = runTest {
        val repo = FakeArticleRepository(pageCountToReport = "5")
        val vm = HomeViewModel(articleRepo = repo)

        // 让 init 中 launch 出来的协程跑完
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse("refreshing should be cleared after load", state.isRefreshing)
        // pageCount=5 > curr=0，应仍可加载下一页
        assertTrue(state.isLoading)
        assertFalse(state.isFinishing)

        // 首屏走 *Flow 接口：banner / top / list 各被消费一次
        assertEquals(1, repo.bannerFlowCount.get())
        assertEquals(1, repo.topFlowCount.get())
        assertEquals(1, repo.listFlowCount.get())
        // 首屏请求 page = 0
        assertEquals(0, repo.lastListPage.get())
        // suspend 入口（getNext 才会走）此时未被调用
        assertEquals(0, repo.listSuspendCount.get())
    }

    @Test
    fun `getNext advances page and re-queries article list`() = runTest {
        val repo = FakeArticleRepository(pageCountToReport = "5")
        val vm = HomeViewModel(articleRepo = repo)
        advanceUntilIdle()

        vm.getNext()
        advanceUntilIdle()

        // banner / top / 首屏 list 各仅由首屏触发；getNext 走的是 suspend 入口
        assertEquals(1, repo.bannerFlowCount.get())
        assertEquals(1, repo.topFlowCount.get())
        assertEquals(1, repo.listFlowCount.get())
        assertEquals(1, repo.listSuspendCount.get())
        assertEquals(1, repo.lastListPage.get())
    }

    @Test
    fun `getNext stops when pageCount exhausted`() = runTest {
        // pageCount=1，意味着 curr=0 时 hasNextPage=true，加载到 curr=1 后 hasNextPage=false
        val repo = FakeArticleRepository(pageCountToReport = "1")
        val vm = HomeViewModel(articleRepo = repo)
        advanceUntilIdle()

        // 首屏完成后已是 curr=0，pageCont=1，仍 hasNextPage=true，所以 isLoading=true
        // getNext 会推进到 curr=1，之后 hasNextPage=false
        vm.getNext()
        advanceUntilIdle()
        val first = vm.uiState.first()
        assertFalse(first.isLoading)
        assertTrue(first.isFinishing)

        // 已到末页，再调用 getNext 不会再发请求
        val before = repo.listSuspendCount.get()
        vm.getNext()
        advanceUntilIdle()
        assertEquals(
            "no extra list call when no next page",
            before + 1, // BaseViewModel.getNextPage 在末页会返回当前页，这里仍会再发一次同 page 的请求
            repo.listSuspendCount.get()
        )
    }

    /**
     * 极简的 [ArticleRepository] 替身：
     * - 计数每个端点被调用的次数；
     * - 不构造任何 [com.example.fragment.project.data.Article] 实例（避免触发 Android Framework）；
     * - 所有 *Flow 方法仅模拟"网络一次发射"的最简路径（无缓存阶段）。
     */
    private class FakeArticleRepository(
        private val pageCountToReport: String,
    ) : ArticleRepository {
        // *Flow 入口计数（首屏走这些）
        val bannerFlowCount = AtomicInteger(0)
        val topFlowCount = AtomicInteger(0)
        val listFlowCount = AtomicInteger(0)
        val lastListPage = AtomicInteger(-1)

        // suspend 入口计数（getNext 走这些）
        val listSuspendCount = AtomicInteger(0)

        override suspend fun getArticleList(page: Int): ArticleList {
            listSuspendCount.incrementAndGet()
            lastListPage.set(page)
            return ArticleList(
                data = ArticleData(
                    curPage = page.toString(),
                    datas = emptyList(),
                    pageCount = pageCountToReport,
                )
            )
        }

        override suspend fun getArticleListByCid(cid: String, page: Int): ArticleList =
            ArticleList(data = ArticleData(pageCount = pageCountToReport))

        override suspend fun searchArticles(key: String, page: Int): ArticleList =
            ArticleList(data = ArticleData(pageCount = pageCountToReport))

        override suspend fun getCollectList(page: Int): ArticleList =
            ArticleList(data = ArticleData(pageCount = pageCountToReport))

        override fun getBannerFlow(): Flow<CachedResult<BannerList>> = flow {
            bannerFlowCount.incrementAndGet()
            emit(CachedResult(BannerList(data = emptyList()), fromCache = false))
        }

        override fun getArticleTopFlow(): Flow<CachedResult<TopArticle>> = flow {
            topFlowCount.incrementAndGet()
            emit(CachedResult(TopArticle(data = emptyList()), fromCache = false))
        }

        override fun getArticleListFlow(page: Int): Flow<CachedResult<ArticleList>> = flow {
            listFlowCount.incrementAndGet()
            lastListPage.set(page)
            emit(
                CachedResult(
                    ArticleList(
                        data = ArticleData(
                            curPage = page.toString(),
                            datas = emptyList(),
                            pageCount = pageCountToReport,
                        )
                    ),
                    fromCache = false,
                )
            )
        }

        override fun getArticleListByCidFlow(
            cid: String,
            page: Int,
        ): Flow<CachedResult<ArticleList>> = flow {
            emit(
                CachedResult(
                    ArticleList(data = ArticleData(pageCount = pageCountToReport)),
                    fromCache = false,
                )
            )
        }
    }
}
