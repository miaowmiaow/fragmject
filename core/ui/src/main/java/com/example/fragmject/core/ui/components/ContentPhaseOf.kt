package com.example.fragmject.core.ui.components

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.example.fragmject.core.ui.utils.RefreshableUiState

/**
 * 从 Paging 的加载态推导首屏四态（纯函数，不触碰 Compose，便于 JVM 单测）。
 *
 * `itemCount == 0` 有两种完全不同的含义，必须区分：
 *
 * 1. **还没开始加载**：首次订阅建立前，`LazyPagingItems` 的 `refresh` / `append` 都是
 *    `NotLoading(endOfPaginationReached = false)`。此时应当显示骨架屏；
 *    早期实现把它判成 Empty，导致冷启动先闪一帧「无数据」再进骨架屏（安装后首次打开最明显）。
 * 2. **已加载完且确实没有数据**：`append.endOfPaginationReached == true`。
 *    依据在数据源侧：`BasePagingSource.load` 里 `over = result.over || result.items.isEmpty()`，
 *    空列表必然 `nextKey = null`，于是 append 到底 —— 这才是真正的空态。
 *
 * 另有一处连带影响：`HomeScreen` 的 `ReportFullyDrawnWhen(phase != ContentPhase.Loading)`
 * 依赖本函数的结果。若首帧被判成 Empty，`reportFullyDrawn()` 会报在空态上，
 * Macrobenchmark 的 `timeToFullDisplay` 量的将是「空态绘制完成」而非「内容可用」。
 */
fun contentPhaseOf(loadStates: CombinedLoadStates, itemCount: Int): ContentPhase {
    val refresh = loadStates.refresh
    val finishedWithoutData =
        (refresh as? LoadState.NotLoading)?.endOfPaginationReached == true ||
            (loadStates.append as? LoadState.NotLoading)?.endOfPaginationReached == true
    return when {
        refresh is LoadState.Loading && itemCount == 0 -> ContentPhase.Loading
        refresh is LoadState.Error && itemCount == 0 -> ContentPhase.Error
        itemCount > 0 -> ContentPhase.Content
        // 0 条 + 已到底：接口确实返回了空
        finishedWithoutData -> ContentPhase.Empty
        // 0 条 + 尚未加载：等同加载中，显示骨架屏
        else -> ContentPhase.Loading
    }
}

/**
 * 从 [RefreshableUiState] 推导首屏四态（Room 通道），与 [contentPhaseOf]（Paging 通道）成对。
 *
 * 两条通道区分「空」的依据不同，但结论必须一致——空态只允许出现在**确实拿到了数据、且数据为空**时：
 *
 * - Paging 通道靠 `itemCount == 0 && endOfPaginationReached`；
 * - Room 通道靠「已经进入 [RefreshableUiState.Success] 且 [isEmpty] 为真」。
 *   [RefreshableUiState.Loading] 本身就是「还没结果」的信号，空态由 `collectAndRefresh` 保证
 *   不会在首帧出现（`refreshed == false` 时空数据退化为 Loading）。
 *
 * @param isEmpty 判定「这一帧数据是否为空」，列表场景传 `{ it.isEmpty() }`；
 *   非集合类型没有空语义，传 `{ false }` 即永不进空态。
 */
fun <T> refreshablePhaseOf(
    state: RefreshableUiState<T>,
    isEmpty: (T) -> Boolean,
): ContentPhase = when (state) {
    is RefreshableUiState.Loading -> ContentPhase.Loading
    is RefreshableUiState.Error -> ContentPhase.Error
    is RefreshableUiState.Success ->
        if (isEmpty(state.data)) ContentPhase.Empty else ContentPhase.Content
}
