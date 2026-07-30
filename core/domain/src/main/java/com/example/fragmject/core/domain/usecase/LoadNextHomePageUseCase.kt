package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.HomeRepository
import com.example.fragmject.core.data.repository.PageData
import com.example.fragmject.core.model.Article
import javax.inject.Inject

/**
 * 加载首页下一页数据。
 *
 * 对 [HomeRepository.loadNextPage] 的返回做语义化包装：
 * - 若网络返回 null（失败/无更多），则 [LoadNextResult.articles] 为空、[LoadNextResult.hasMore] 为 false。
 * - 否则根据当前页码与总页数计算 [LoadNextResult.hasMore]。
 */
class LoadNextHomePageUseCase @Inject constructor(
    private val homeRepo: HomeRepository,
) {
    suspend operator fun invoke(page: Int): LoadNextResult {
        val data = homeRepo.loadNextPage(page)
        return if (data == null) {
            LoadNextResult(emptyList(), null, false)
        } else {
            val hasMore = page < (data.pageCount ?: 0)
            LoadNextResult(data.articles, data.pageCount, hasMore)
        }
    }
}

/** 加载下一页的返回结果。 */
data class LoadNextResult(
    val articles: List<Article>,
    val pageCount: Int?,
    val hasMore: Boolean,
)