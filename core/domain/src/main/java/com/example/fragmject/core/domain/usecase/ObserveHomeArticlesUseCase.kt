package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.model.Article
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 观察首页文章（Room 唯一数据源）。
 *
 * 将 [HomeRepository.observeHomeArticles] 的观察能力上提为 UseCase，
 * 使 ViewModel 不再直接依赖 Repository，统一依赖边界。
 */
class ObserveHomeArticlesUseCase @Inject constructor(
    private val homeRepo: HomeRepository,
) {
    operator fun invoke(): Flow<List<Article>> = homeRepo.observeHomeArticles()
}
