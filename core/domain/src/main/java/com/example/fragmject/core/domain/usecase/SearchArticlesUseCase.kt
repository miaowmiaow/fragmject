package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.ArticlePageResult
import com.example.fragmject.core.domain.result.DomainResult
import javax.inject.Inject

/**
 * 搜索文章（支持分页）。
 */
class SearchArticlesUseCase @Inject constructor(
    private val repo: SearchRepository,
) {
    suspend operator fun invoke(key: String, page: Int): DomainResult<ArticlePageResult> =
        repo.searchArticles(key, page)
}