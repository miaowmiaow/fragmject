package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.MyCoinNextResult
import javax.inject.Inject

/**
 * 加载积分明细下一页。
 */
class LoadNextMyCoinPageUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(page: Int): MyCoinNextResult {
        return repo.getMyCoinList(page)
    }
}