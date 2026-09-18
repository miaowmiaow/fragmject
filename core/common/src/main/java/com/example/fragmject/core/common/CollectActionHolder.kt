package com.example.fragmject.core.common

/**
 * 收藏动作的统一契约。
 *
 * 消除 7 个 ViewModel 重复的 `suspend fun collect(id, collect)` 样板：
 * 各 ViewModel 实现本接口并通过 [collectAction] 暴露收藏动作，
 * UI 层无需再用 `remember(viewModel) { viewModel::collect }` 包装方法引用。
 */
interface CollectActionHolder {
    val collectAction: suspend (String, Boolean) -> Unit
}