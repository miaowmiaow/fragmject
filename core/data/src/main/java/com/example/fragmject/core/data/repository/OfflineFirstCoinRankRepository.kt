package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.Coin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 积分排行榜 Repository：第 1 页进 Room 缓存，后续页走 ViewModel 内存。
 */
class OfflineFirstCoinRankRepository(
    private val commonRepo: CommonRepository,
) {

    companion object {
        const val CACHE_KEY_PAGE_1 = "coin_rank_1"
    }

    /** 只观察第 1 页，不再通过 getByPagePrefix 合并多页。 */
    fun observeCoinRanks(): Flow<List<Coin>> =
        AppDatabase.getCoinRankDao().getByCacheKey(CACHE_KEY_PAGE_1).map { entities ->
            entities.map { it.toDomain() }
        }

    /** 刷新第 1 页：写 Room → Flow 自动推送。返回 pageCount。 */
    suspend fun refreshCoinRank(): Int? {
        val result = runCatching { commonRepo.getCoinRank(1) }
        val data = result.getOrNull()?.data ?: return null
        val datas = data.datas ?: return null
        AppDatabase.getCoinRankDao().replaceAll(CACHE_KEY_PAGE_1,
            datas.mapIndexed { i, coin -> coin.toEntity(CACHE_KEY_PAGE_1, i) })
        return data.pageCount.toIntOrNull()
    }

    /** 加载第 N 页（N>=2）：仅网络，不写 Room。 */
    suspend fun loadCoinRankNextPage(page: Int): CoinRankPageData? {
        val result = runCatching { commonRepo.getCoinRank(page) }
        val data = result.getOrNull()?.data ?: return null
        val datas = data.datas ?: return null
        if (datas.isEmpty()) return null
        return CoinRankPageData(coins = datas, pageCount = data.pageCount.toIntOrNull())
    }
}

data class CoinRankPageData(
    val coins: List<Coin>,
    val pageCount: Int?,
)