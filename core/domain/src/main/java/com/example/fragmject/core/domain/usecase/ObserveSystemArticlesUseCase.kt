package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.model.Article
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 观察体系文章（Room 唯一数据源）。
 */
class ObserveSystemArticlesUseCase @Inject constructor(
    private val systemRepo: SystemRepository,
) {
    operator fun invoke(cid: String): Flow<List<Article>> =
        systemRepo.observeSystemArticles(cid)
}
