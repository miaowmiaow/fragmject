package com.example.fragmject.core.data.contract.remote

import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner

/**
 * 文章远程数据源：首页 / 体系 / 搜索 / 收藏。
 *
 * 定义在 data-contract 层，由 core:network 的 Retrofit 接口（internal ArticleService）
 * 继承并实现；core:data 的 Repository 只依赖本契约，不感知 Retrofit。
 */
interface ArticleRemoteDataSource {
    suspend fun getArticleList(page: Int): DataResponse<ArticleData>
    suspend fun getArticleListByCid(page: Int, cid: String): DataResponse<ArticleData>
    suspend fun searchArticles(key: String, page: Int): DataResponse<ArticleData>
    suspend fun getCollectList(page: Int): DataResponse<ArticleData>
    suspend fun fetchBannerList(): DataResponse<List<Banner>>
    suspend fun fetchArticleTop(): DataResponse<List<Article>>
}
