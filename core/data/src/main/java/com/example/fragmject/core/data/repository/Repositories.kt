package com.example.fragmject.core.data.repository

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleList
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.model.BannerList
import com.example.fragmject.core.model.CoinRank
import com.example.fragmject.core.model.HotKeyList
import com.example.fragmject.core.model.Login
import com.example.fragmject.core.model.MyCoinList
import com.example.fragmject.core.model.NavigationList
import com.example.fragmject.core.model.ProjectTreeList
import com.example.fragmject.core.model.Register
import com.example.fragmject.core.model.ShareArticleList
import com.example.fragmject.core.model.TopArticle
import com.example.fragmject.core.model.TreeList
import com.example.fragmject.core.model.UserCoin
import com.example.fragmject.core.network.http.HttpResponse
import kotlinx.coroutines.flow.Flow

/**
 * 文章相关接口：首页 / 体系 / 搜索 / 收藏。
 *
 * Repository 设计动机：
 * 1. 把网络细节（URL、参数、占位符）封装在 data 层，ViewModel 只关心业务方法名；
 * 2. 接口 + 默认实现，便于单测时通过 Hilt @Bind 注入替身；
 * 3. 实例由 Hilt [RepositoryModule] 按 @Singleton 提供。
 */
interface ArticleRepository {

    /** 首页文章列表，分页（page 从 0 开始）。 */
    suspend fun getArticleList(page: Int): ArticleList

    /** 知识体系下的文章列表（page 从 0 开始）。 */
    suspend fun getArticleListByCid(cid: String, page: Int): ArticleList

    /** 关键字搜索文章（page 从 0 开始） */
    suspend fun searchArticles(key: String, page: Int): ArticleList

    /** 我的收藏列表（page 从 0 开始） */
    suspend fun getCollectList(page: Int): ArticleList

    // ===== 供 OfflineFirstArticleRepository 使用的裸网络方法 =====

    /** 获取 Banner 列表（无缓存，裸网络）。 */
    suspend fun fetchBannerList(): BannerList

    /** 获取置顶文章（无缓存，裸网络）。 */
    suspend fun fetchArticleTop(): TopArticle
}

/**
 * 项目相关接口。
 */
interface ProjectRepository {

    /** 项目列表（page 从 1 开始）。 */
    suspend fun getProjectList(cid: String, page: Int): ArticleList

    // ===== 供 OfflineFirstProjectRepository 使用的裸网络方法 =====

    /** 获取项目分类树（裸网络）。 */
    suspend fun fetchProjectTree(): ProjectTreeList
}

/**
 * 用户/账号相关接口：登录 / 注册 / 退出 / 用户分享。
 */
interface UserRepository {

    suspend fun login(username: String, password: String): Login

    suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): Register

    suspend fun logout(): HttpResponse

    /** 指定用户分享的文章（page 从 1 开始） */
    suspend fun getUserShareArticles(userId: String, page: Int): ShareArticleList
}

/**
 * 当前登录用户「我的」相关接口：积分、分享、新建分享。
 */
interface MyRepository {

    /** 我的积分汇总 */
    suspend fun getUserCoin(): UserCoin

    /** 我的积分明细（page 从 1 开始） */
    suspend fun getMyCoinList(page: Int): MyCoinList

    /** 我分享的文章（page 从 1 开始） */
    suspend fun getMyShareList(page: Int): ShareArticleList

    /** 新增一篇分享 */
    suspend fun shareArticle(title: String, link: String): HttpResponse

    /** 收藏一篇文章（id 为文章 originId） */
    suspend fun collectArticle(id: String): HttpResponse

    /** 取消收藏一篇文章（id 为文章 originId） */
    suspend fun uncollectArticle(id: String): HttpResponse
}

/**
 * 导航 / 体系树 / 热搜词 等公共数据接口（Room 唯一数据源模式）。
 */
interface CommonRepository {

    /** 积分排行榜（page 从 1 开始）。 */
    suspend fun getCoinRank(page: Int): CoinRank

    // ===== 供 OfflineFirstCommonRepository 使用的裸网络方法 =====

    /** 获取导航列表（裸网络）。 */
    suspend fun fetchNavigation(): NavigationList

    /** 获取体系树（裸网络）。 */
    suspend fun fetchSystemTree(): TreeList

    /** 获取热搜词（裸网络）。 */
    suspend fun fetchHotKey(): HotKeyList
}

/**
 * 首页聚合层接口：Room 唯一数据源模式。
 *
 * ViewModel 调用方只需：
 * 1. init 中 [observeHomeArticles] + [observeHomeBanners] 观察 Room Flow
 * 2. init 中触发一次 [refreshHome] 拉网络写库（仅页面 0 写 Room）
 * 3. [loadNextPage] 拉下一页网络数据，ViewModel 自行拼入内存列表
 */
interface HomeRepository {
    fun observeHomeArticles(): Flow<List<Article>>
    fun observeHomeBanners(): Flow<List<Banner>>
    suspend fun refreshHome(): Int?
    suspend fun loadNextPage(page: Int): PageData?
}

/**
 * 加载更多返回的数据：文章列表 + 总页数（用于分页判断）。
 */
data class PageData(
    val articles: List<Article>,
    val pageCount: Int?,
)

/**
 * 体系文章聚合层接口：Room 唯一数据源模式。
 *
 * ViewModel 调用方只需：
 * 1. init 中 [observeSystemArticles] 观察 Room Flow（第 0 页）
 * 2. init 中触发一次 [refreshSystemArticles] 拉网络写库
 * 3. [loadSystemNextPage] 拉下一页网络数据，ViewModel 自行拼入内存列表
 */
interface SystemRepository {
    fun observeSystemArticles(cid: String): Flow<List<Article>>
    suspend fun refreshSystemArticles(cid: String): Int?
    suspend fun loadSystemNextPage(cid: String, page: Int): PageData?
}

/**
 * 搜索聚合层接口：将底层 [ArticleRepository.searchArticles] 返回包装为 [AppResult]。
 */
interface SearchRepository {
    suspend fun searchArticles(key: String, page: Int): com.example.fragmject.core.common.result.AppResult<ArticleList>
}

/**
 * 用户中心聚合层接口。
 */
interface UserCenterRepository {
    suspend fun getUserShareArticles(userId: String, page: Int): com.example.fragmject.core.common.result.AppResult<ShareArticleList>
}

/**
 * 我的收藏聚合层接口：Room 唯一数据源模式。
 */
interface MyCollectRepository {
    fun observeMyCollect(): Flow<List<Article>>
    suspend fun refreshMyCollect(): Int?
    suspend fun loadMyCollectNextPage(page: Int): PageData?
}