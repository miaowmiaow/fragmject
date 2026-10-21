package com.example.fragmject.feature.home.ui.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import com.example.fragmject.core.ui.components.BottomNavItem
import com.example.fragmject.core.model.HotKey

/**
 * 三种宽度布局共用的入参：导航项 + 当前选中项 + 热词 + 首页列表滚动状态。
 *
 * 合并成一个持有者，避免每个布局变体重复同一组参数；[LazyListState] 是稳定对象，
 * 故用 @Stable 而非 @Immutable。
 */
@Stable
internal class MainPaneSpec(
    val navItems: List<BottomNavItem>,
    val navIndex: Int,
    val hotKeyResult: List<HotKey>,
    val homeListState: LazyListState,
)
