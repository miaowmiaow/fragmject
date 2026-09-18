package com.example.fragmject.core.data.repository

import com.example.fragmject.core.network.datasource.CommonDataSource

import com.example.fragmject.core.database.dao.CoinRankDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.CoinRankRepository
import com.example.fragmject.core.domain.result.CoinRankPageData
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Coin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 积分排行榜 Repository：第 1 页进 Room 缓存，后续页走 ViewModel 内存。
 */
@Singleton
class OfflineFirstCoinRankRepository @Inject constructor(
    private val coinRankDao: CoinRankDao,
    private val commonRepo: CommonDataSource,
) : CoinRankRepository {

    companion object {
        const val CACHE_KEY_PAGE_1 = "coin_rank_1"
    }

    /** 只观察第 1 页，不再通过 getByPagePrefix 合并多页。 */
    override fun observeCoinRanks(): Flow<List<Coin>> =
        coinRankDao.getByCacheKey(CACHE_KEY_PAGE_1).map { entities ->
            entities.map { it.toDomain() }
        }

    /** 刷新第 1 页：写 Room → Flow 自动推送。 */
    override suspend fun refreshCoinRank(): DomainResult<Int> {
        return fetchAsDomainResult(
            call = { commonRepo.getCoinRank(1) },
        ) { resp ->
            val data = resp.data
            val datas = data?.datas.orEmpty()
            if (datas.isNotEmpty()) {
                coinRankDao.replaceAll(
                    CACHE_KEY_PAGE_1,
                    datas.mapIndexed { i, coin -> coin.toEntity(CACHE_KEY_PAGE_1, i) })
            }
            data?.pageCount?.toIntOrNull() ?: 0
        }
    }

    /** 加载第 N 页（N>=2）：仅网络，不写 Room。 */
    override suspend fun loadCoinRankNextPage(page: Int): DomainResult<CoinRankPageData> {
        return fetchAsDomainResult(
            call = { commonRepo.getCoinRank(page) },
        ) { resp ->
            CoinRankPageData(
                coins = resp.data?.datas.orEmpty(),
                pageCount = resp.data?.pageCount?.toIntOrNull(),
            )
        }
    }
}