package com.example.fragmject.core.navigation.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * 为大屏两栏（Expanded）的 DetailPane 提供按 key 隔离的 [ViewModelStoreOwner]。
 *
 * 背景：DetailPane 的内容渲染在 `NavDisplay` 之外，拿不到 Navigation 3 的 entry 级
 * ViewModelStore（`rememberViewModelStoreNavEntryDecorator` 只作用于返回栈上的 `NavEntry`）。
 * 若不做处理，详情面板的 ViewModel 会退化成 Activity 作用域，带来两个问题：
 * - 与单栏模式（entry 级）行为不一致；
 * - 切换不同详情 key 时复用同一个 ViewModel，可能读到上一篇文章的残留状态。
 *
 * 本实现让 DetailPane 与单栏保持同一语义：ViewModel 随 [key] 变化重建，
 * 旧 key 的 [ViewModelStore] 被 clear 释放，等价于 entry 出栈。
 *
 * 同时实现 [HasDefaultViewModelProviderFactory] 并复用父 owner 的默认工厂，
 * 保证 Hilt 注入链路（`hiltViewModel()`）在 DetailPane 内同样可用。
 */
@Composable
public fun ProvideDetailPaneViewModelStore(
    key: Any?,
    content: @Composable () -> Unit,
) {
    // 父 owner 通常是 Activity：复用其默认工厂（Hilt 的 ViewModelFactory 由此而来）
    val parentOwner = LocalViewModelStoreOwner.current
    val owner = remember(key, parentOwner) {
        if (key == null) null else DetailPaneViewModelStoreOwner(parentOwner)
    }
    // key 变化或离开组合时清理旧 store，避免 ViewModel 泄漏
    DisposableEffect(key) {
        onDispose { owner?.viewModelStore?.clear() }
    }
    if (owner == null) {
        content()
        return
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}

/** DetailPane 专用的 [ViewModelStoreOwner]：持有随 key 生命周期的独立 [ViewModelStore]。 */
private class DetailPaneViewModelStoreOwner(
    // 父 owner 可能为 null（LocalViewModelStoreOwner 是 nullable 的 CompositionLocal）
    parent: ViewModelStoreOwner?,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
            ?: ViewModelProvider.NewInstanceFactory()
}
