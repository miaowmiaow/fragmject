package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.repository.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.repository.paging.UserSharePagingSource
import com.example.fragmject.core.data.repository.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.domain.result.getOrNull
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UserCenterRepository] 领域端口的 data 层适配器。
 */
@Singleton
class UserCenterRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
) : UserCenterRepository {

    // 约定：积分读取失败降级为「无数据」；取消信号原样传播（旧 runCatching 会把取消吞成 null）
    override suspend fun getUserCoin(userId: String): Coin? =
        fetchAsDomainResult(call = { userRemoteDataSource.getUserShareArticles(userId, 1) }) {
            it.data?.coinInfo
        }.getOrNull()

    override fun getUserSharePagingData(userId: String): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { UserSharePagingSource(userId, userRemoteDataSource) },
    ).flow
}
