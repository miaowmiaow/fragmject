package com.example.fragmject.core.data.contract.remote

import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.ProjectTree

/**
 * 项目远程数据源：项目列表 / 项目树。
 *
 * 定义在 data-contract 层，由 core:network 的 Retrofit 接口（internal ProjectService）
 * 继承并实现。
 */
interface ProjectRemoteDataSource {
    suspend fun getProjectList(page: Int, cid: String): DataResponse<ArticleData>
    suspend fun fetchProjectTree(): DataResponse<List<ProjectTree>>
}
