package com.example.fragmject.core.network.service

import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.network.datasource.ProjectDataSource
import com.example.fragmject.core.network.http.DataResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ProjectService : ProjectDataSource {

    @GET("project/list/{page}/json")
    override suspend fun getProjectList(
        @Path("page") page: Int,
        @Query("cid") cid: String,
    ): DataResponse<ArticleData>

    @GET("project/tree/json")
    override suspend fun fetchProjectTree(): DataResponse<List<ProjectTree>>
}
