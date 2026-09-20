package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.paging.CoinRankPagingSource
import com.example.fragmject.core.domain.repository.CoinRankRepository
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.network.datasource.CommonDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CoinRankRepository] 领域端口的 data 层适配器。
 *
 * 纯网络分页，直接透传 [CommonDataSource.getCoinRank]。
 */
@Singleton
class CoinRankRepositoryImpl @Inject constructor(
    private val remote: CommonDataSource,
) : CoinRankRepository {

    override fun getCoinRankPagingData(): Flow<PagingData<Coin>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { CoinRankPagingSource(remote) },
    ).flow
}
