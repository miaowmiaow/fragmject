package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.paging.UserSharePagingSource
import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.network.datasource.UserRemoteDataSource
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

    override suspend fun getUserCoin(userId: String): Coin? {
        return runCatching { userRemoteDataSource.getUserShareArticles(userId, 1) }
            .getOrNull()
            ?.data
            ?.coinInfo
    }

    override fun getUserSharePagingData(userId: String): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { UserSharePagingSource(userId, userRemoteDataSource) },
    ).flow
}
