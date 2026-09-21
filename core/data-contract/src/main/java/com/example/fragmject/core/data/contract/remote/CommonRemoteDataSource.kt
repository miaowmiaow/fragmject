package com.example.fragmject.core.data.contract.remote

import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.model.CoinRankData
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree

/**
 * 公共远程数据源：导航 / 体系树 / 热搜词 / 积分排行榜。
 *
 * 定义在 data-contract 层，由 core:network 的 Retrofit 接口（internal CommonService）
 * 继承并实现。
 */
interface CommonRemoteDataSource {
    suspend fun getCoinRank(page: Int): DataResponse<CoinRankData>
    suspend fun fetchNavigation(): DataResponse<MutableList<Navigation>>
    suspend fun fetchSystemTree(): DataResponse<MutableList<Tree>>
    suspend fun fetchHotKey(): DataResponse<List<HotKey>>
}
