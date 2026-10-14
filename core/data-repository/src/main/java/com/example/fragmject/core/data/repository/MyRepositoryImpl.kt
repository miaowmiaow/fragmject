package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.repository.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.repository.paging.MyCoinPagingSource
import com.example.fragmject.core.data.repository.paging.MySharePagingSource
import com.example.fragmject.core.data.repository.util.fetchAsDomainResult
import com.example.fragmject.core.data.repository.util.fetchAsSimpleResult
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.domain.result.getOrNull
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
 * 消化 [MyRemoteDataSource] 返回的 DataResponse，转换为领域结果类型；
 * 业务码判断与异常处理统一经 util 层的 fetchAs* 入口，本类不再手写 try/catch 与 `"0"` 判断。
 */
@Singleton
class MyRepositoryImpl @Inject constructor(
    private val remote: MyRemoteDataSource,
) : MyRepository {

    // 约定：积分读取失败降级为「无数据」，由 UI 展示默认值
    override suspend fun getUserCoin(): Coin? =
        fetchAsDomainResult(call = { remote.getUserCoin() }) { it.data }.getOrNull()

    override fun getMyCoinPagingData(): Flow<PagingData<MyCoin>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { MyCoinPagingSource(remote) },
    ).flow

    override fun getMySharePagingData(): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { MySharePagingSource(remote) },
    ).flow

    override suspend fun shareArticle(title: String, link: String): SimpleResult =
        fetchAsSimpleResult(failureMessage = "分享失败") { remote.shareArticle(title, link) }

    override suspend fun collectArticle(id: String): SimpleResult =
        fetchAsSimpleResult(failureMessage = "收藏失败") { remote.collectArticle(id) }

    override suspend fun uncollectArticle(id: String): SimpleResult =
        fetchAsSimpleResult(failureMessage = "取消收藏失败") { remote.uncollectArticle(id) }
}
