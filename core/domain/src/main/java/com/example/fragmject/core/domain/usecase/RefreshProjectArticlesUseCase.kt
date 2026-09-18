package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import javax.inject.Inject

/**
 * 刷新项目分类文章：触发网络拉取并写 Room 缓存。
 *
 * @return 领域结果：成功携带总页数，失败携带错误码与消息。
 */
class RefreshProjectArticlesUseCase @Inject constructor(
    private val projectRepo: ProjectRepository,
) {
    suspend operator fun invoke(cid: String): DomainResult<Int> =
        projectRepo.refreshProjectArticles(cid)
}
