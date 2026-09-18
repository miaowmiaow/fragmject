package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.model.Article
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 观察项目分类文章（Room 唯一数据源）。
 */
class ObserveProjectArticlesUseCase @Inject constructor(
    private val projectRepo: ProjectRepository,
) {
    operator fun invoke(cid: String): Flow<List<Article>> =
        projectRepo.observeProjectArticles(cid)
}
