package com.example.fragmject.core.network.service

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.network.datasource.ArticleDataSource
import com.example.fragmject.core.network.http.DataResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ArticleService : ArticleDataSource {

    @GET("article/list/{page}/json")
    override suspend fun getArticleList(@Path("page") page: Int): DataResponse<ArticleData>

    @GET("article/list/{page}/json")
    override suspend fun getArticleListByCid(
        @Path("page") page: Int,
        @Query("cid") cid: String,
    ): DataResponse<ArticleData>

    @FormUrlEncoded
    @POST("article/query/{page}/json")
    override suspend fun searchArticles(
        @Field("k") key: String,
        @Path("page") page: Int,
    ): DataResponse<ArticleData>

    @GET("lg/collect/list/{page}/json")
    override suspend fun getCollectList(@Path("page") page: Int): DataResponse<ArticleData>

    @GET("banner/json")
    override suspend fun fetchBannerList(): DataResponse<List<Banner>>

    @GET("article/top/json")
    override suspend fun fetchArticleTop(): DataResponse<List<Article>>
}
