package com.example.fragmject.core.ui.components

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import com.example.fragmject.core.ui.utils.RefreshableUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [contentPhaseOf] 的判定测试。
 *
 * 为什么值得单测：这是「首屏闪空态」类问题的唯一真值来源。历史上 `itemCount == 0`
 * 被直接判成 Empty，导致冷启动先显示无数据、再显示骨架屏、最后才出内容；
 * 区分依据只有 `endOfPaginationReached`，极易在后续重构中被改丢。
 */
class ContentPhaseOfTest {

    @Test
    fun `first frame before any load is treated as loading`() {
        // 订阅建立前的初始态：refresh / append 都是 NotLoading(false)
        val states = combined(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        )

        assertEquals(ContentPhase.Loading, contentPhaseOf(states, itemCount = 0))
    }

    @Test
    fun `refreshing with no cached data is loading`() {
        val states = combined(refresh = LoadState.Loading)

        assertEquals(ContentPhase.Loading, contentPhaseOf(states, itemCount = 0))
    }

    @Test
    fun `failed first load is error`() {
        val states = combined(refresh = LoadState.Error(IllegalStateException("boom")))

        assertEquals(ContentPhase.Error, contentPhaseOf(states, itemCount = 0))
    }

    @Test
    fun `loaded empty response is empty`() {
        // 服务端返回空列表：BasePagingSource 会置 nextKey = null，append 到底
        val states = combined(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        )

        assertEquals(ContentPhase.Empty, contentPhaseOf(states, itemCount = 0))
    }

    @Test
    fun `loaded data is content`() {
        val states = combined(refresh = LoadState.NotLoading(endOfPaginationReached = true))

        assertEquals(ContentPhase.Content, contentPhaseOf(states, itemCount = 20))
    }

    @Test
    fun `pull to refresh keeps existing list visible`() {
        // 下拉刷新时 refresh 是 Loading，但列表里有旧数据：不应退回骨架屏
        val states = combined(refresh = LoadState.Loading)

        assertEquals(ContentPhase.Content, contentPhaseOf(states, itemCount = 10))
    }

    @Test
    fun `refreshable loading is loading`() {
        val state: RefreshableUiState<List<String>> = RefreshableUiState.Loading

        assertEquals(ContentPhase.Loading, refreshablePhaseOf(state) { it.isEmpty() })
    }

    @Test
    fun `refreshable failure is error`() {
        val state: RefreshableUiState<List<String>> = RefreshableUiState.Error("boom")

        assertEquals(ContentPhase.Error, refreshablePhaseOf(state) { it.isEmpty() })
    }

    @Test
    fun `refreshable success with empty data is empty`() {
        // 接口确实返回空：只有这一种情况允许显示空态
        val state: RefreshableUiState<List<String>> = RefreshableUiState.Success(emptyList())

        assertEquals(ContentPhase.Empty, refreshablePhaseOf(state) { it.isEmpty() })
    }

    @Test
    fun `refreshable success with data is content`() {
        val state: RefreshableUiState<List<String>> = RefreshableUiState.Success(listOf("a"))

        assertEquals(ContentPhase.Content, refreshablePhaseOf(state) { it.isEmpty() })
    }

    @Test
    fun `refreshable non collection never goes empty`() {
        // 非集合类型没有「空」语义：始终内容态
        val state: RefreshableUiState<Int> = RefreshableUiState.Success(0)

        assertEquals(ContentPhase.Content, refreshablePhaseOf(state) { false })
    }

    private fun combined(
        refresh: LoadState,
        prepend: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
    ): CombinedLoadStates = CombinedLoadStates(
        refresh = refresh,
        prepend = prepend,
        append = append,
        source = LoadStates(refresh, prepend, append),
    )
}
