package com.example.fragmject.core.ui.utils

import com.example.fragmject.core.domain.result.DomainResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 可刷新列表的统一三态状态模型。
 *
 * 收敛此前并存的三种状态建模（Success 内嵌 isLoading 二态、独立 Loading 三态、多 StateFlow），
 * 统一为 Loading / Success / Error，供「Room 唯一数据源 + 网络刷新」类 ViewModel 复用。
 */
sealed interface RefreshableUiState<out T> {
    data object Loading : RefreshableUiState<Nothing>
    data class Success<T>(val data: T) : RefreshableUiState<T>
    data class Error(val message: String) : RefreshableUiState<Nothing>
}

/** 取数据：Success 返回 data，Loading / Error 返回空列表（空态/错误态由 UI 层用 when 判定）。 */
fun <T> RefreshableUiState<List<T>>.dataOrEmpty(): List<T> =
    (this as? RefreshableUiState.Success)?.data.orEmpty()

/**
 * 统一「observe(本地缓存) + refresh(网络)」编排，含错误态优先与首帧空态抑制。
 *
 * - observe 流发射时，若当前为 Error 则跳过（错误态优先，直到显式 retry）；
 * - **首次 refresh 结束前，空数据不转 Success**：Room 首帧就会发射空列表，
 *   直接转 `Success(empty)` 会让空态抢在骨架屏前面闪一帧（NavScreen / ProjectScreen 复现：
 *   进入页面先看到「无数据」，再看到骨架屏，最后才出内容）。空态只能由
 *   「接口确实返回了空」产生，因此这里退化为 [RefreshableUiState.Loading]；
 * - refresh 成功不直接写数据，依赖 refresh 回写 Room 触发 observe 重发；
 *   但若 observe 没有重发（接口返回空、数据与首帧一致），会**重新查一次本地库**并把仍停留在
 *   Loading 的状态补成 Success，避免永远卡在骨架屏。这里必须重查而不是复用最后一个观测值：
 *   refresh 内部写库虽已完成（`onSuccess` 是 suspend，返回前已提交），但 Room 的
 *   invalidation → 重查 → 发射是异步的，复用旧值会再次闪一帧空态。
 *
 * @param isEmpty 判定「这一帧数据是否为空」，由调用方提供（列表场景传 `{ it.isEmpty() }`）。
 *   默认 false：非集合类型没有「空」语义，也就不需要抑制。
 */
fun <T> CoroutineScope.collectAndRefresh(
    observe: Flow<T>,
    refresh: suspend () -> DomainResult<Unit>,
    state: MutableStateFlow<RefreshableUiState<T>>,
    isEmpty: (T) -> Boolean = { false },
) {
    // 首次 refresh 是否已结束（成功/失败都算结束）。
    val refreshed = MutableStateFlow(false)

    launch {
        observe.collect { data ->
            state.update { cur ->
                when {
                    cur is RefreshableUiState.Error -> cur
                    // refreshed 在 update 内部读取：CAS 重试时会重新取值，
                    // 避免与下面的补发互相覆盖（补发可能先落到 state 上）
                    !refreshed.value && isEmpty(data) -> RefreshableUiState.Loading
                    else -> RefreshableUiState.Success(data)
                }
            }
        }
    }
    launch {
        when (val r = refresh()) {
            is DomainResult.Success -> {
                refreshed.value = true
                // 必须重新查一次本地库，不能复用 latest.value：
                // refresh 内部已经把数据写进 Room（onSuccess 是 suspend，返回前已提交），
                // 但 Room 的 invalidation → 重新查询 → 发射是异步的，此时 latest.value
                // 仍是刷新前的空列表，直接发布就会闪一帧空态，随后才被真实数据覆盖。
                val fresh = observe.first()
                state.update { cur ->
                    if (cur is RefreshableUiState.Loading) RefreshableUiState.Success(fresh) else cur
                }
            }

            is DomainResult.Failure -> {
                refreshed.value = true
                state.update { RefreshableUiState.Error(r.message) }
            }
        }
    }
}

/**
 * 错误态重试：转 Loading 后重新刷新，成功后由 observe 流（refresh 回写 Room）转 Success。
 *
 * 为什么成功后要主动取一次 [observe]：Room 的 Flow 对相同结果不会重发（内部有
 * distinctUntilChanged）。若重试前后数据一致（常见：接口返回空），observe 不会发射，
 * 状态会永久停在骨架屏。这里取一次最新值补发，保证重试必然收敛到 Success / Error。
 */
fun <T> CoroutineScope.retryRefresh(
    observe: Flow<T>,
    refresh: suspend () -> DomainResult<Unit>,
    state: MutableStateFlow<RefreshableUiState<T>>,
) {
    launch {
        state.update { RefreshableUiState.Loading }
        when (val r = refresh()) {
            is DomainResult.Success -> {
                val data = observe.first()
                state.update { cur ->
                    if (cur is RefreshableUiState.Loading) RefreshableUiState.Success(data) else cur
                }
            }

            is DomainResult.Failure -> state.update { RefreshableUiState.Error(r.message) }
        }
    }
}
