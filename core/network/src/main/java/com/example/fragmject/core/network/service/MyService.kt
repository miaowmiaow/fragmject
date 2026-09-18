package com.example.fragmject.core.network.service

import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoinData
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.network.datasource.MyRemoteDataSource
import com.example.fragmject.core.network.http.DataResponse
import com.example.fragmject.core.network.http.HttpResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface MyService : MyRemoteDataSource {

    @GET("lg/coin/userinfo/json")
    override suspend fun getUserCoin(): DataResponse<Coin>

    @GET("lg/coin/list/{page}/json")
    override suspend fun getMyCoinList(@Path("page") page: Int): DataResponse<MyCoinData>

    @GET("user/lg/private_articles/{page}/json")
    override suspend fun getMyShareList(@Path("page") page: Int): DataResponse<ShareArticle>

    @FormUrlEncoded
    @POST("lg/user_article/add/json")
    override suspend fun shareArticle(
        @Field("title") title: String,
        @Field("link") link: String,
    ): HttpResponse

    @POST("lg/collect/{id}/json")
    override suspend fun collectArticle(@Path("id") id: String): HttpResponse

    @POST("lg/uncollect_originId/{id}/json")
    override suspend fun uncollectArticle(@Path("id") id: String): HttpResponse
}
