package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.MyCollectRepository
import javax.inject.Inject

/**
 * 加载收藏的下一页。
 */
class LoadNextMyCollectPageUseCase @Inject constructor(
    private val repo: MyCollectRepository,
) {
    suspend operator fun invoke(page: Int): LoadNextResult {
        val data = repo.loadMyCollectNextPage(page)
        return if (data == null) {
            LoadNextResult(emptyList(), null, false)
        } else {
            LoadNextResult(data.articles, data.pageCount, page < (data.pageCount ?: 0))
        }
    }
}