package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.data.repository.MyRepository
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
            coin = uc.data,
            coinList = mcl.data?.datas?.toList().orEmpty(),
            pageCount = mcl.data?.pageCount?.toInt(),
        )
    }
}

data class MyCoinHomeResult(
    val coin: Coin? = null,
    val coinList: List<MyCoin> = emptyList(),
    val pageCount: Int? = null,
)