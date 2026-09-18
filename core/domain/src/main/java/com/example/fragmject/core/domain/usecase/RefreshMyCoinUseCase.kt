package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.MyCoinHomeResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 初次加载积分页：并行获取积分汇总 + 积分明细第 1 页。
 */
class RefreshMyCoinUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(page: Int): MyCoinHomeResult = coroutineScope {
        val userCoin = async { repo.getUserCoin() }
        val myCoinList = async { repo.getMyCoinList(page) }
        val uc = userCoin.await()
        val mcl = myCoinList.await()
        MyCoinHomeResult(
            coin = uc,
            coinList = mcl.items,
            pageCount = mcl.pageCount,
        )
    }
}