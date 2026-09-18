package com.example.fragmject.core.data.repository

import com.example.fragmject.core.network.datasource.MyRemoteDataSource

import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.CollectResult
import com.example.fragmject.core.domain.result.MyCoinNextResult
import com.example.fragmject.core.domain.result.MyShareResult
import com.example.fragmject.core.domain.result.ShareArticleResult
import com.example.fragmject.core.model.Coin
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

    override suspend fun getMyCoinList(page: Int): MyCoinNextResult {
        val resp = remote.getMyCoinList(page)
        val datas = resp.data?.datas?.toList().orEmpty()
        return MyCoinNextResult(
            items = datas,
            pageCount = resp.data?.pageCount?.toInt(),
            isEmpty = datas.isEmpty(),
        )
    }

    override suspend fun getMyShareList(page: Int): MyShareResult {
        val resp = remote.getMyShareList(page)
        val data = resp.data?.shareArticles
        val datas = data?.datas.orEmpty()
        return MyShareResult(
            articles = datas,
            pageCount = data?.pageCount?.toInt(),
            hasMore = datas.isNotEmpty(),
        )
    }

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
