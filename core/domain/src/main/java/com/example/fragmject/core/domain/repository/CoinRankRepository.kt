package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.CoinRankPageData
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Coin
import kotlinx.coroutines.flow.Flow

/**
 * 积分排行榜领域端口（第 1 页进 Room，后续页走内存）。
 */
interface CoinRankRepository {
    fun observeCoinRanks(): Flow<List<Coin>>
    suspend fun refreshCoinRank(): DomainResult<Int>
    suspend fun loadCoinRankNextPage(page: Int): DomainResult<CoinRankPageData>
}
