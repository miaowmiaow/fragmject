package com.example.fragmject.core.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.impl.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.impl.paging.MyCoinPagingSource
import com.example.fragmject.core.data.impl.paging.MySharePagingSource
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.CollectResult
import com.example.fragmject.core.domain.result.ShareArticleResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MyRepository] 领域端口的 data 层适配器。
 *
 * 消化 [MyRemoteDataSource] 返回的 DataResponse，转换为领域结果类型。
 */
@Singleton
class MyRepositoryImpl @Inject constructor(
    private val remote: MyRemoteDataSource,
) : MyRepository {

    override suspend fun getUserCoin(): Coin? = remote.getUserCoin().data

    override fun getMyCoinPagingData(): Flow<PagingData<MyCoin>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { MyCoinPagingSource(remote) },
    ).flow

    override fun getMySharePagingData(): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { MySharePagingSource(remote) },
    ).flow

    override suspend fun shareArticle(title: String, link: String): ShareArticleResult {
        val resp = remote.shareArticle(title, link)
        return ShareArticleResult(
            success = resp.errorCode == "0",
            message = resp.errorMsg,
        )
    }

    override suspend fun collectArticle(id: String): CollectResult {
        val resp = remote.collectArticle(id)
        return CollectResult(success = resp.errorCode == "0", message = resp.errorMsg)
    }

    override suspend fun uncollectArticle(id: String): CollectResult {
        val resp = remote.uncollectArticle(id)
        return CollectResult(success = resp.errorCode == "0", message = resp.errorMsg)
    }
}
