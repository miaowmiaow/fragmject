package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.SystemRepository
import javax.inject.Inject

/**
 * 刷新指定体系分类的文章缓存。
 */
class RefreshSystemArticlesUseCase @Inject constructor(
    private val repo: SystemRepository,
) {
    suspend operator fun invoke(cid: String): Int? = repo.refreshSystemArticles(cid)
}