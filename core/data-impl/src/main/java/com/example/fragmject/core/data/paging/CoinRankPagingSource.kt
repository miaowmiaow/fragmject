package com.example.fragmject.core.data.impl.paging

import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource

/**
 * 积分排行榜分页源（page 从 1 开始）。
 */
class CoinRankPagingSource(
    private val remote: CommonRemoteDataSource,
) : BasePagingSource<Coin>(startPage = 1) {

    override suspend fun fetchPage(page: Int): PageData<Coin> {
        val data = remote.getCoinRank(page).data
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
