package com.example.fragmject.core.network.service

import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.User
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface UserService : UserRemoteDataSource {

    @FormUrlEncoded
    @POST("user/login")
    override suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
    ): DataResponse<User>

    @FormUrlEncoded
    @POST("user/register")
    override suspend fun register(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("repassword") repassword: String,
    ): DataResponse<User>

    @GET("user/logout/json")
    override suspend fun logout(): HttpResponse

    @GET("user/{id}/share_articles/{page}/json")
    override suspend fun getUserShareArticles(
        @Path("id") userId: String,
        @Path("page") page: Int,
    ): DataResponse<ShareArticle>
}
