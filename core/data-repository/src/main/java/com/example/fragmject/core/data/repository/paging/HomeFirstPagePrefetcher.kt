package com.example.fragmject.core.data.repository.paging

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页第一页的进程级预取。
 *
 * ============================ 为什么需要 ============================
 * 首页数据层是纯网络（无本地缓存），`PagingSource` 的首次 `load` 只能等到
 * `HomeScreen` 首次组合、`collectAsLazyPagingItems()` 建立订阅之后才发出。
 * 也就是说网络往返完全排在「Activity 启动 + 首帧绘制」之后串行执行，
 * Macrobenchmark 上表现为 `timeToFullDisplay - timeToInitialDisplay ≈ 500ms`
 * 的骨架屏停留。
 *
 * 这里把第一页请求提前到 `Application.onCreate` 发起，让网络与 Activity 启动
 * **并行**：首屏订阅建立时直接消费已完成的预取结果，把这段串行等待压成 0。
 *
 * ============================ 边界与回退 ============================
 * - 只预取第 0 页：[HomePagingSource] 的后续分页（key > 0）仍走原路径；
 * - 一次性消费：被 `await` 取走后即清空，下拉刷新 / 配置变更重建 PagingSource
 *   不会拿到陈旧数据；
 * - 超过 [PREFETCH_TTL_NANOS] 的预取视为过期（例如用户很久之后才进首页），
 *   直接丢弃并重新请求，避免把「几分钟前的首页」当首屏渲染；
 * - 预取失败（无网、服务端错误）不抛给调用方：`await` 捕获后回退到一次正常请求，
 *   这样失败语义与「没有预取」完全一致，不会把错误态提前固化。
 */
@Singleton
class HomeFirstPagePrefetcher @Inject constructor(
    private val remote: ArticleRemoteDataSource,
    private val appScope: AppCoroutineScope,
) {

    private val lock = Any()

    /** 尚未被消费的预取任务；为 null 表示没有在途预取。 */
    private var pending: Deferred<PageData<Article>>? = null

    /** 预取发起时刻（[System.nanoTime]），用于判断是否过期。 */
    private var startedAtNanos = 0L

    /**
     * 发起第一页预取。
     *
     * 可重复调用（例如后续接入多入口预热）：已有在途任务时直接返回，不会重复发请求。
     */
    fun warmUp() {
        synchronized(lock) {
            if (pending != null) return
            startedAtNanos = System.nanoTime()
            pending = appScope.async { fetchFirstPage() }
        }
    }

    /**
     * 消费预取结果；没有可用预取时退化为一次普通的第一页请求。
     *
     * 调用方（[HomePagingSource]）不需要区分「命中预取」与「回退请求」：
     * 两种情况返回的都是同一结构的 [PageData]，失败时抛出与原来一致的异常。
     */
    suspend fun awaitFirstPage(): PageData<Article> {
        val candidate = synchronized(lock) {
            val deferred = pending
            pending = null
            deferred?.takeIf {
                !it.isCancelled && System.nanoTime() - startedAtNanos <= PREFETCH_TTL_NANOS
            }
        } ?: return fetchFirstPage()

        return try {
            candidate.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 预取失败不能影响首屏语义：重新请求一次，让错误按原有路径冒泡到 LoadResult.Error。
            fetchFirstPage()
        }
    }

    private suspend fun fetchFirstPage(): PageData<Article> {
        val data = remote.getArticleList(FIRST_PAGE).requirePagingData()
        return PageData(
            items = data.datas.requirePagingField("data.datas"),
            over = data.over,
        )
    }

    private companion object {
        /** 首页起始页，与 [BasePagingSource.startPage] 保持一致。 */
        const val FIRST_PAGE = 0

        /** 预取有效期：超过则重新请求，避免首屏拿到过期内容。 */
        const val PREFETCH_TTL_NANOS = 5_000_000_000L
    }
}
