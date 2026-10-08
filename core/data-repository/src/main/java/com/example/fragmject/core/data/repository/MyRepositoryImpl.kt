package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.repository.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.repository.paging.MyCoinPagingSource
import com.example.fragmject.core.data.repository.paging.MySharePagingSource
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource
import kotlinx.coroutines.CancellationException
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

    override suspend fun getUserCoin(): Coin? =
        try {
            remote.getUserCoin().data
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    override fun getMyCoinPagingData(): Flow<PagingData<MyCoin>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { MyCoinPagingSource(remote) },
    ).flow

    override fun getMySharePagingData(): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { MySharePagingSource(remote) },
    ).flow

    override suspend fun shareArticle(title: String, link: String): SimpleResult {
        val resp = try {
            remote.shareArticle(title, link)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return SimpleResult(success = false, message = "分享失败")
        }
        return SimpleResult(
            success = resp.errorCode == "0",
            message = resp.errorMsg,
        )
    }

    override suspend fun collectArticle(id: String): SimpleResult {
        val resp = try {
            remote.collectArticle(id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return SimpleResult(success = false, message = "收藏失败")
        }
        return SimpleResult(success = resp.errorCode == "0", message = resp.errorMsg)
    }

    override suspend fun uncollectArticle(id: String): SimpleResult {
        val resp = try {
            remote.uncollectArticle(id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return SimpleResult(success = false, message = "取消收藏失败")
        }
        return SimpleResult(success = resp.errorCode == "0", message = resp.errorMsg)
    }
}