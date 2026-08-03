package com.example.fragmject.core.common.viewmodel

import androidx.lifecycle.ViewModel
import java.util.concurrent.ConcurrentHashMap

abstract class BaseViewModel : ViewModel() {

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
            nextPage = if (state.currPage < state.pageCont) state.currPage + 1 else state.currPage
            state.copy(currPage = nextPage)
        }
        return nextPage
    }

    fun hasNextPage(key: String = DEFAULT_KEY): Boolean {
        val state = pageStateMap[key] ?: PageState()
        return state.currPage < state.pageCont
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
}