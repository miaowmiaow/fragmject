package com.example.fragment.project.ui.main.home

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.ArticleList
import com.example.fragment.project.data.BannerList
import com.example.fragment.project.data.TopArticle
import com.example.fragment.project.data.repository.ArticleRepository
import com.example.fragment.project.data.repository.CachedResult
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 不可变 UiState：所有字段均为 val，list 通过 copy 时生成新引用，
 * 这样 StateFlow 的 distinctUntilChanged 才能正确触发 Compose 重组。
 */
data class HomeUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val result: List<Article> = emptyList(),
)

class HomeViewModel(
    // 通过默认参数注入 Repository，单测时可传入替身实现
    private val articleRepo: ArticleRepository = WanRepositoryProvider.article,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    /**
     * 首页接入 SWR：banner / top / 首页文章三路 Flow 并发收集，按"阶段"合并发射。
     *
     * 旧实现使用 `combine` 汇聚三路：任一路（缓存或网络）抖动都会触发 UiState 更新，
     * 极端情况下首屏会经历 "banner 缓存 → list 缓存 → banner 网络 → list 网络" 等多达 4 次重组。
     *
     * 新实现把 6 次潜在 emit（3 路 × cache/network 各一次）压缩为最多 2 次 UiState 更新：
     * 1. **缓存阶段**：等三路缓存阶段全部就位（拿到缓存 / 确认无缓存）→ 至少有一路命中缓存才发射；
     * 2. **网络阶段**：等三路网络阶段全部完成（成功 / 失败均算结束）→ 必发射一次最终态。
     *
     * 注意：仅首页（page=0）走缓存；下拉刷新会重置分页并复用此方法，从而再次命中缓存上屏，
     * 然后被网络刷新覆盖。下一页（getNext）属于增量加载，不接入缓存。
     *
     * @param userTriggered 是否由用户主动触发（下拉刷新）。
     *  - true：保留 isRefreshing 直到网络返回，给出明确的"刷新中"反馈；
     *  - false（默认）：自动加载场景。缓存命中后会立即把页面当作"已就绪"，
     *    避免出现"缓存数据已渲染、顶部却仍挂着下拉刷新动画"的体验割裂。
     */
    fun getHome(userTriggered: Boolean = false) {
        _uiState.update {
            it.copy(isRefreshing = userTriggered, isLoading = false, isFinishing = false)
        }
        val homePage = getHomePage()
        viewModelScope.launch {
            coroutineScope {
                val bannerSource = SwrSource<BannerList>().also { it.launchCollect(this, articleRepo.getBannerFlow()) }
                val topSource = SwrSource<TopArticle>().also { it.launchCollect(this, articleRepo.getArticleTopFlow()) }
                val listSource = SwrSource<ArticleList>().also { it.launchCollect(this, articleRepo.getArticleListFlow(homePage)) }

                // 1) 缓存阶段：并发等待三路"缓存就位"信号；只要有一路命中缓存就合并上屏一次
                listOf(bannerSource.cacheReady, topSource.cacheReady, listSource.cacheReady).awaitAll()
                val anyCacheHit = bannerSource.cache != null || topSource.cache != null || listSource.cache != null
                if (anyCacheHit) {
                    val articleData = assembleArticles(bannerSource.cache, topSource.cache, listSource.cache)
                    _uiState.update { it.copy(result = articleData) }
                }

                // 2) 网络阶段：并发等待三路网络阶段全部结束（无论成功失败），合并后做最终 UiState 更新
                listOf(bannerSource.networkReady, topSource.networkReady, listSource.networkReady).awaitAll()
                // 网络阶段以网络返回为准，缺失（失败）则回退到缓存值，保证可用性
                val banner = bannerSource.network ?: bannerSource.cache
                val top = topSource.network ?: topSource.cache
                val list = listSource.network ?: listSource.cache
                val articleData = assembleArticles(banner, top, list)
                // 仅在网络结果到达时更新分页元信息，避免缓存阶段用过期 pageCount 误判 hasNextPage
                updatePageCont(list?.data?.pageCount?.toInt())
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isLoading = hasNextPage(),
                        isFinishing = !hasNextPage(),
                        result = articleData,
                    )
                }
            }
        }
    }

    /** 把 banner / top / list 三路最新值拼装成 UI 列表；任一为 null 时跳过对应分块。 */
    private fun assembleArticles(
        banner: BannerList?,
        top: TopArticle?,
        list: ArticleList?,
    ): List<Article> {
        val articleData = mutableListOf<Article>()
        banner?.data?.let { articleData.add(Article(banners = it, viewType = 0)) }
        // top 通过 copy 生成新对象，避免就地改字段，保持类的稳定性
        top?.data?.map { it.copy(top = true) }?.let { articleData.addAll(it) }
        list?.data?.datas?.let { articleData.addAll(it) }
        return articleData
    }

    fun getNext() {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        viewModelScope.launch {
            val response = articleRepo.getArticleList(getNextPage())
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                val appended = response.data?.datas
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    // 始终生成新 List 引用，确保订阅方能感知列表变化
                    result = if (appended.isNullOrEmpty()) state.result else state.result + appended
                )
            }
        }
    }
}

/**
 * 单路 SWR 流的"阶段栅栏"。把 [CachedFetch] 的两次 emit 拆成两个可 await 的信号点，
 * 让多路流可以在外部按"全部就位再统一发射"的节奏聚合，避免 combine 的中间态抖动。
 *
 * 状态机：
 * - 收到 fromCache=true → 写入 [cache]，触发 [cacheReady]；继续等网络
 * - 收到 fromCache=false → 写入 [network]，未触发的 [cacheReady] 此时也一并触发（说明无缓存）；触发 [networkReady]
 * - 流结束（缓存命中但网络失败 / 缓存与网络都缺失）→ 兜底触发尚未触发的两个信号，避免外部死等
 */
private class SwrSource<T : com.example.miaow.base.http.HttpResponse> {
    @Volatile var cache: T? = null
    @Volatile var network: T? = null
    val cacheReady: CompletableDeferred<Unit> = CompletableDeferred()
    val networkReady: CompletableDeferred<Unit> = CompletableDeferred()

    fun launchCollect(scope: kotlinx.coroutines.CoroutineScope, flow: Flow<CachedResult<T>>): Job =
        scope.launch {
            try {
                flow.collect { result ->
                    if (result.fromCache) {
                        cache = result.value
                        cacheReady.complete(Unit)
                    } else {
                        network = result.value
                        // 若此前没有缓存 emit，这里同时把缓存阶段标记结束（无缓存）
                        cacheReady.complete(Unit)
                        networkReady.complete(Unit)
                    }
                }
            } finally {
                // 流正常结束或异常退出都要兜底解除等待，避免 await 永远挂起
                cacheReady.complete(Unit)
                networkReady.complete(Unit)
            }
        }
}