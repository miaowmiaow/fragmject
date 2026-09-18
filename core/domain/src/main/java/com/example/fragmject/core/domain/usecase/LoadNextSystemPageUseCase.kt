package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import javax.inject.Inject

/**
 * 加载体系分类的下一页文章。透传领域结果，分页判断上移到 ViewModel。
 */
class LoadNextSystemPageUseCase @Inject constructor(
    private val repo: SystemRepository,
) {
    suspend operator fun invoke(cid: String, page: Int): DomainResult<PageData> =
        repo.loadSystemNextPage(cid, page)
}