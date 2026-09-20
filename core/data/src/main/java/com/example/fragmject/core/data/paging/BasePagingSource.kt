package com.example.fragmject.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException

/**
 * 分页数据统一视图：屏蔽不同接口返回结构中 [items]/[over] 的具体差异。
 *
 * @param items 当前页数据列表
 * @param over 服务端是否已到末页
 */
data class PageData<T : Any>(
    val items: List<T>,
    val over: Boolean,
)

/**
 * 基于 page 的 [PagingSource] 基类，收敛所有「纯网络分页源」的重复逻辑：
 * - 起始页、prev/next key 计算、空列表兜底；
 * - [CancellationException] 原样向上抛出，避免吞掉协程取消信号。
 *
 * 子类只需提供 [startPage] 与 [fetchPage]（一次页请求 + data 解包），
 * 不再各自实现 load / getRefreshKey。
 */
abstract class BasePagingSource<T : Any>(
    private val startPage: Int,
) : PagingSource<Int, T>() {

    /** 加载第 [page] 页并返回统一分页视图。 */
    protected abstract suspend fun fetchPage(page: Int): PageData<T>

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: startPage
        return try {
            val result = fetchPage(page)
            val over = result.over || result.items.isEmpty()
            LoadResult.Page(
                data = result.items,
                prevKey = if (page > startPage) page - 1 else null,
                nextKey = if (over) null else page + 1,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}
