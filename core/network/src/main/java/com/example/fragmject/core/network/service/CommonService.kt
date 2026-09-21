package com.example.fragmject.core.network.service

import com.example.fragmject.core.model.CoinRankData
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.contract.model.DataResponse
import retrofit2.http.GET
import retrofit2.http.Path

internal interface CommonService : CommonRemoteDataSource {

    @GET("coin/rank/{page}/json")
    override suspend fun getCoinRank(@Path("page") page: Int): DataResponse<CoinRankData>

    @GET("navi/json")
    override suspend fun fetchNavigation(): DataResponse<MutableList<Navigation>>

    @GET("tree/json")
    override suspend fun fetchSystemTree(): DataResponse<MutableList<Tree>>

    @GET("hotkey/json")
    override suspend fun fetchHotKey(): DataResponse<List<HotKey>>
}
