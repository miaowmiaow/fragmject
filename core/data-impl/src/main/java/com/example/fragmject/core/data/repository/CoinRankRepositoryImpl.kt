package com.example.fragmject.core.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.impl.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.impl.paging.CoinRankPagingSource
import com.example.fragmject.core.domain.repository.CoinRankRepository
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CoinRankRepository] 领域端口的 data 层适配器。
 *
 * 纯网络分页，直接透传 [CommonRemoteDataSource.getCoinRank]。
 */
@Singleton
class CoinRankRepositoryImpl @Inject constructor(
    private val remote: CommonRemoteDataSource,
) : CoinRankRepository {

    override fun getCoinRankPagingData(): Flow<PagingData<Coin>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { CoinRankPagingSource(remote) },
    ).flow
}
