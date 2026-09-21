package com.example.fragmject.core.data.contract.remote

import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoinData
import com.example.fragmject.core.model.ShareArticle

/**
 * 我的远程数据源：积分 / 分享 / 收藏。
 *
 * 定义在 data-contract 层，由 core:network 的 Retrofit 接口（internal MyService）
 * 继承并实现。
 */
interface MyRemoteDataSource {
    suspend fun getUserCoin(): DataResponse<Coin>
    suspend fun getMyCoinList(page: Int): DataResponse<MyCoinData>
    suspend fun getMyShareList(page: Int): DataResponse<ShareArticle>
    suspend fun shareArticle(title: String, link: String): HttpResponse
    suspend fun collectArticle(id: String): HttpResponse
    suspend fun uncollectArticle(id: String): HttpResponse
}
