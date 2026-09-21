package com.example.fragmject.core.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.impl.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.impl.paging.MyCollectPagingSource
import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MyCollectRepository] 领域端口的 data 层适配器。
 *
 * 纯网络分页，与我的分享页一致：直接透传 [ArticleRemoteDataSource.getCollectList]。
 */
@Singleton
class MyCollectRepositoryImpl @Inject constructor(
    private val remote: ArticleRemoteDataSource,
) : MyCollectRepository {

    override fun getMyCollectPagingData(): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { MyCollectPagingSource(remote) },
    ).flow
}
