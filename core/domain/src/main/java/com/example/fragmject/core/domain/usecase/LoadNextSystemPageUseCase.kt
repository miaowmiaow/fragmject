package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.SystemRepository
import javax.inject.Inject

/**
 * 加载体系分类的下一页文章。
 */
class LoadNextSystemPageUseCase @Inject constructor(
    private val repo: SystemRepository,
) {
    suspend operator fun invoke(cid: String, page: Int): LoadNextResult {
        val data = repo.loadSystemNextPage(cid, page)
        return if (data == null) {
            LoadNextResult(emptyList(), null, false)
        } else {
            LoadNextResult(data.articles, data.pageCount, page < (data.pageCount ?: 0))
        }
    }
}