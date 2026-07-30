package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.data.repository.MyRepository
import javax.inject.Inject

/**
 * 加载积分明细下一页。
 */
class LoadNextMyCoinPageUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(page: Int): MyCoinNextResult {
        val response = repo.getMyCoinList(page)
        val datas = response.data?.datas?.toList().orEmpty()
        return MyCoinNextResult(
            items = datas,
            pageCount = response.data?.pageCount?.toInt(),
            isEmpty = datas.isEmpty(),
        )
    }
}

data class MyCoinNextResult(
    val items: List<MyCoin> = emptyList(),
    val pageCount: Int? = null,
    val isEmpty: Boolean = true,
)