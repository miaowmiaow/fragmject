package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import javax.inject.Inject

/**
 * 加载首页下一页数据。透传领域结果，分页判断上移到 ViewModel。
 */
class LoadNextHomePageUseCase @Inject constructor(
    private val homeRepo: HomeRepository,
) {
    suspend operator fun invoke(page: Int): DomainResult<PageData> = homeRepo.loadNextPage(page)
}