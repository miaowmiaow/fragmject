package com.example.fragmject.core.common.viewmodel

import androidx.lifecycle.ViewModel
import java.util.concurrent.ConcurrentHashMap

abstract class BaseViewModel : ViewModel() {

    /** 分页收尾状态：封装「更新页码 → 计算是否还有下一页」的结果。 */
    data class PagingState(val isLoading: Boolean, val isFinishing: Boolean)

    companion object {
        const val DEFAULT_KEY = "null"
        const val DEFAULT_VALUE = 0
    }

    private data class PageState(
        val homePage: Int = DEFAULT_VALUE,
        val currPage: Int = DEFAULT_VALUE,
        val pageCont: Int = DEFAULT_VALUE
    )

    /**
     * 以 key 维度存储分页状态，并使用 ConcurrentHashMap.compute 做原子更新，
     * 避免跨多个 Map 读-改-写导致的竞态条件。
     */
    private val pageStateMap = ConcurrentHashMap<String, PageState>()

    /**
     * 初始化分页：将 home/curr 置为 page，pageCont 暂置为 page+1
     * 命名上 "getHomePage" 略有歧义，但为避免破坏既有调用方暂保留。
     * page：首页初始值
     */
    fun getHomePage(page: Int = DEFAULT_VALUE, key: String = DEFAULT_KEY): Int {
        pageStateMap.compute(key) { _, _ ->
            PageState(homePage = page, currPage = page, pageCont = page + 1)
        }
        return page
    }

    fun isHomePage(key: String = DEFAULT_KEY): Boolean {
        val state = pageStateMap[key] ?: PageState()
        return state.homePage == state.currPage
    }

    /**
     * 获取下一页
     */
    fun getNextPage(key: String = DEFAULT_KEY): Int {
        var nextPage = DEFAULT_VALUE
        pageStateMap.compute(key) { _, old ->
            val state = old ?: PageState()
            // 用「已加载页数」而非「当前页号」与总页数比较：
            // 项目同时存在 0-based（Home/System/MyCollect，页 0..N-1）与 1-based（Project，页 1..N）分页，
            // 旧的 `currPage < pageCont` 在 0-based 场景下会多请求一页越界页（off-by-one）。
            val loadedCount = state.currPage - state.homePage + 1
            nextPage = if (loadedCount < state.pageCont) state.currPage + 1 else state.currPage
            state.copy(currPage = nextPage)
        }
        return nextPage
    }

    fun hasNextPage(key: String = DEFAULT_KEY): Boolean {
        val state = pageStateMap[key] ?: PageState()
        val loadedCount = state.currPage - state.homePage + 1
        return loadedCount < state.pageCont
    }

    /**
     * 更新总页码
     */
    fun updatePageCont(pageCont: Int?, key: String = DEFAULT_KEY) {
        pageStateMap.compute(key) { _, old ->
            val state = old ?: PageState()
            state.copy(pageCont = pageCont ?: DEFAULT_VALUE)
        }
    }

    /**
     * 分页收尾：更新总页码后，返回统一的「加载中 / 已结束」状态，
     * 收敛各 ViewModel 中重复的 updatePageCont + hasNextPage + 三元判断。
     */
    fun finishPage(pageCount: Int?, key: String = DEFAULT_KEY): PagingState {
        updatePageCont(pageCount, key)
        val hasNext = hasNextPage(key)
        return PagingState(isLoading = hasNext, isFinishing = !hasNext)
    }

    /**
     * 加载下一页的统一编排：
     * getNextPage → fetch(page) → 成功 finishPage / 失败回退页码。
     * 失败判断由调用方在 fetch 内完成（返回 [PageResult.success] = false）。
     * 成功时用 [finishPage] 的结果覆盖 [PageResult.hasMore]，保证与页码状态机一致。
     */
    protected suspend fun <T> loadNextPage(
        key: String = DEFAULT_KEY,
        fetch: suspend (page: Int) -> PageResult<T>,
    ): PageResult<T> {
        val page = getNextPage(key)
        val result = fetch(page)
        if (result.success) {
            val paging = finishPage(result.pageCount, key)
            return result.copy(hasMore = paging.isLoading)
        }
        updatePageCont(page - 1, key)
        return result
    }
}

/**
 * 分页加载的统一结果契约：将各 usecase 的返回结构归一化。
 * - [items]：本页数据
 * - [pageCount]：总页数（可能为 null）
 * - [hasMore]：是否还有下一页
 * - [success]：本页加载是否成功（false 表示失败/无更多，触发页码回退）
 */
data class PageResult<T>(
    val items: List<T> = emptyList(),
    val pageCount: Int? = null,
    val hasMore: Boolean = false,
    val success: Boolean = true,
)