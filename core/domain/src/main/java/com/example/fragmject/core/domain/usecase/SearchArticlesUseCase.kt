package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.common.result.AppResult
import com.example.fragmject.core.data.repository.SearchRepository
import com.example.fragmject.core.model.ArticleList
import javax.inject.Inject

/**
 * 搜索文章（支持分页）。
 */
class SearchArticlesUseCase @Inject constructor(
    private val repo: SearchRepository,
) {
    suspend operator fun invoke(key: String, page: Int): AppResult<ArticleList> =
        repo.searchArticles(key, page)
}