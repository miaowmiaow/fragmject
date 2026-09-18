package com.example.fragmject.core.network.datasource

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.CoinRankData
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.MyCoinData
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.model.User
import com.example.fragmject.core.network.http.DataResponse
import com.example.fragmject.core.network.http.HttpResponse

/**
 * 网络数据源契约（对外暴露的纯抽象接口，不含任何 Retrofit 注解）。
 *
 * 这些接口定义在 core:network，供 core:data 的 Repository 依赖；
 * 具体的 Retrofit 注解接口（[com.example.fragmject.core.network.service] 下的 XxxService）
 * 以 internal 方式继承本契约并动态代理实现，从而 core:data 无需引入 retrofit2。
 */

/** 文章相关数据源：首页 / 体系 / 搜索 / 收藏。 */
interface ArticleDataSource {
    suspend fun getArticleList(page: Int): DataResponse<ArticleData>
    suspend fun getArticleListByCid(page: Int, cid: String): DataResponse<ArticleData>
    suspend fun searchArticles(key: String, page: Int): DataResponse<ArticleData>
    suspend fun getCollectList(page: Int): DataResponse<ArticleData>
    suspend fun fetchBannerList(): DataResponse<List<Banner>>
    suspend fun fetchArticleTop(): DataResponse<List<Article>>
}

/** 项目相关数据源。 */
interface ProjectDataSource {
    suspend fun getProjectList(page: Int, cid: String): DataResponse<ArticleData>
    suspend fun fetchProjectTree(): DataResponse<List<ProjectTree>>
}

/** 公共数据源：导航 / 体系树 / 热搜词 / 积分排行榜。 */
interface CommonDataSource {
    suspend fun getCoinRank(page: Int): DataResponse<CoinRankData>
    suspend fun fetchNavigation(): DataResponse<MutableList<Navigation>>
    suspend fun fetchSystemTree(): DataResponse<MutableList<Tree>>
    suspend fun fetchHotKey(): DataResponse<List<HotKey>>
}

/** 用户远程数据源：登录 / 注册 / 登出 / 用户分享。 */
interface UserRemoteDataSource {
    suspend fun login(username: String, password: String): DataResponse<User>
    suspend fun register(username: String, password: String, repassword: String): DataResponse<User>
    suspend fun logout(): HttpResponse
    suspend fun getUserShareArticles(userId: String, page: Int): DataResponse<ShareArticle>
}

/** 我的远程数据源：积分 / 分享 / 收藏。 */
interface MyRemoteDataSource {
    suspend fun getUserCoin(): DataResponse<Coin>
    suspend fun getMyCoinList(page: Int): DataResponse<MyCoinData>
    suspend fun getMyShareList(page: Int): DataResponse<ShareArticle>
    suspend fun shareArticle(title: String, link: String): HttpResponse
    suspend fun collectArticle(id: String): HttpResponse
    suspend fun uncollectArticle(id: String): HttpResponse
}
