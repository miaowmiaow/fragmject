package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import javax.inject.Inject

/**
 * 加载项目分类文章下一页。透传领域结果，分页判断上移到 ViewModel。
 */
class LoadNextProjectPageUseCase @Inject constructor(
    private val projectRepo: ProjectRepository,
) {
    suspend operator fun invoke(cid: String, page: Int): DomainResult<PageData> =
        projectRepo.loadProjectNextPage(cid, page)
}
