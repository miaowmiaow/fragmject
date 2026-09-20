package com.example.fragmject.core.domain.repository

import androidx.paging.PagingData
import com.example.fragmject.core.model.Coin
import kotlinx.coroutines.flow.Flow

/**
 * 积分排行榜领域端口：纯网络分页。
 */
interface CoinRankRepository {
    fun getCoinRankPagingData(): Flow<PagingData<Coin>>
}
