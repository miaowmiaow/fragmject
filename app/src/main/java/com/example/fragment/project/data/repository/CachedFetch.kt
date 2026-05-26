package com.example.fragment.project.data.repository

import com.example.miaow.base.http.CoroutineHttp
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.HttpResponse
import com.example.miaow.base.http.ResponseCache
import com.example.miaow.base.utils.GSonUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * SWR（Stale-While-Revalidate）算子：先盘读、再走网络。
 *
 * 设计动机：WanAndroid 接口没有标准 HTTP 缓存头，OkHttp 磁盘缓存对业务接口形同虚设。
 * 这里在 Repository 层之上做一个轻量算子，让 ViewModel 用一行接入"先缓存上屏，再网络刷新"。
 *
 * 工作流：
 * 1. 命中且 errorCode == "0" 的缓存先发射一次（[CachedResult.fromCache] = true）；
 * 2. 调用网络再发射一次（[CachedResult.fromCache] = false）；
 * 3. 网络结果 errorCode == "0" 时异步写盘，失败/异常不覆盖旧缓存；
 * 4. 不接入：写操作（POST/PUT）、登录态强相关数据、分页非首页。
 *
 * cacheKey 的产生：直接用 [HttpRequest.getUrl] 派生（path 替换 + query 拼接后的相对 URL），
 * 与实际网络请求 URL 严格一致，从而让 cacheKey 与请求参数永远同步——上层只需写一份请求 DSL。
 *
 * ViewModel 通过 [CachedResult.fromCache] 区分两次 emit 的语义：
 * - 缓存阶段：通常只用来"快速上屏"，不要触发依赖最新元数据的副作用（如分页计数、转场动画结束信号）；
 * - 网络阶段：才是真正可信的数据，更新分页 / 关 loading / 触发后续逻辑。
 */
data class CachedResult<T : HttpResponse>(
    val value: T,
    /** true = 来自磁盘缓存的快速回放；false = 来自网络的最终结果 */
    val fromCache: Boolean,
)

/**
 * 带 SWR 缓存的 GET 请求。
 *
 * @param init 请求 DSL，复用 Repository 里 `httpGet { ... }` 的同一份配置。
 *             cacheKey 会从这份 DSL 自动派生，因此**不需要也不应该**手动指定。
 */
inline fun <reified T : HttpResponse> cachedHttpGet(
    noinline init: HttpRequest.() -> Unit,
): Flow<CachedResult<T>> = cachedFlow(
    init = init,
    fetch = { request -> CoroutineHttp.getInstance().get(request, T::class.java) },
    typeOf = T::class.java,
)

/**
 * 带 SWR 缓存的 POST 请求。注意 POST 多为写操作，仅在确为"读语义的 POST"时使用。
 */
inline fun <reified T : HttpResponse> cachedHttpPost(
    noinline init: HttpRequest.() -> Unit,
): Flow<CachedResult<T>> = cachedFlow(
    init = init,
    fetch = { request -> CoroutineHttp.getInstance().post(request, T::class.java) },
    typeOf = T::class.java,
)

@PublishedApi
internal fun <T : HttpResponse> cachedFlow(
    init: HttpRequest.() -> Unit,
    fetch: suspend (HttpRequest) -> T,
    typeOf: Class<T>,
): Flow<CachedResult<T>> = flow {
    // 同一个 HttpRequest 既用于派生 cacheKey、又用于发起网络请求，
    // 避免对 init lambda 重复 apply 导致 time 字段漂移、以及参数序列化两次。
    val request = HttpRequest().apply(init)
    val cacheKey = request.getUrl()

    // 1. 先尝试读缓存
    val cached = ResponseCache.read(cacheKey, typeOf)
    val hasCacheEmitted = cached != null && cached.errorCode == "0"
    if (hasCacheEmitted) {
        emit(CachedResult(cached, fromCache = true))
    }
    // 2. 再走网络；弱网场景下不能让异常冲掉已发射的缓存，否则
    //    上层 collect 会提前终止，isRefreshing 等 UI 状态永远不会被释放。
    val freshResult = runCatching { fetch(request) }
    val fresh = freshResult.getOrNull()
    if (fresh != null) {
        emit(CachedResult(fresh, fromCache = false))
        // 3. 网络成功才回写缓存，避免错误响应污染缓存
        if (fresh.errorCode == "0") {
            try {
                ResponseCache.writeAsync(cacheKey, GSonUtils.lazyAwareGson.toJson(fresh))
            } catch (_: Exception) {
                // 序列化异常不应影响主流程
            }
        }
    } else if (!hasCacheEmitted) {
        // 缓存未命中且网络抛异常：上抛，交由上层决定如何重试 / 提示用户
        throw freshResult.exceptionOrNull() ?: IllegalStateException("unknown network error")
    }
    // 缓存已 emit 但网络失败：默默吞掉，保证已上屏的内容稳定，不闪烁。
}

/**
 * SWR 通用 collect 算子。封装"缓存阶段不动 isLoading / 网络阶段才更新分页"的统一约定，
 * 让所有接入 SWR 的 ViewModel 不再重复编写 `if (!fromCache) {...}` 分支。
 *
 * @param onCache  缓存命中时回调；fresh-only 的副作用（如分页计数更新）请勿放在这里。
 * @param onNetwork 网络阶段（无论成功失败上抛）回调；可在此把 isRefreshing 切回 false、更新分页。
 *
 * 用法举例：
 * ```
 * repo.getXxxFlow(key).collectCached(
 *     onCache = { v -> _ui.update { it.copy(result = v.data) } },
 *     onNetwork = { v ->
 *         updatePageCont(v.data?.pageCount?.toInt())
 *         _ui.update { it.copy(result = v.data, isRefreshing = false) }
 *     },
 * )
 * ```
 */
suspend inline fun <T : com.example.miaow.base.http.HttpResponse> kotlinx.coroutines.flow.Flow<CachedResult<T>>.collectCached(
    crossinline onCache: (T) -> Unit = {},
    crossinline onNetwork: (T) -> Unit,
) = collect { result ->
    if (result.fromCache) onCache(result.value) else onNetwork(result.value)
}