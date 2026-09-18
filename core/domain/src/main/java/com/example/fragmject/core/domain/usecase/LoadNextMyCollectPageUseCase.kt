package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import javax.inject.Inject

/**
 * 加载收藏的下一页。透传领域结果，分页判断上移到 ViewModel。
 */
class LoadNextMyCollectPageUseCase @Inject constructor(
    private val repo: MyCollectRepository,
) {
    suspend operator fun invoke(page: Int): DomainResult<PageData> =
        repo.loadMyCollectNextPage(page)
}