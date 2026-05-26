package com.example.fragment.project.data.repository

import com.example.fragment.project.data.ArticleList
import com.example.fragment.project.data.BannerList
import com.example.fragment.project.data.CoinRank
import com.example.fragment.project.data.HotKeyList
import com.example.fragment.project.data.Login
import com.example.fragment.project.data.MyCoinList
import com.example.fragment.project.data.NavigationList
import com.example.fragment.project.data.ProjectTreeList
import com.example.fragment.project.data.Register
import com.example.fragment.project.data.ShareArticleList
import com.example.fragment.project.data.TopArticle
import com.example.fragment.project.data.TreeList
import com.example.fragment.project.data.UserCoin
import com.example.miaow.base.http.HttpResponse
import kotlinx.coroutines.flow.Flow

/**
 * 文章相关接口：首页 / 体系 / 搜索 / 收藏。
 *
 * Repository 设计动机：
 * 1. 把网络细节（URL、参数、占位符）封装在 data 层，ViewModel 只关心业务方法名；
 * 2. 接口 + 默认实现，便于后续替换 mock / 单测；
 * 3. 不引入 DI 框架，使用极轻量的 [WanRepositoryProvider] 单例工厂。
 */
interface ArticleRepository {

    /** 首页文章列表，分页（page 从 0 开始）。首页（page=0）请改用 [getArticleListFlow] 走 SWR 缓存。 */
    suspend fun getArticleList(page: Int): ArticleList

    /** 知识体系下的文章列表（page 从 0 开始）。首页请改用 [getArticleListByCidFlow] 走 SWR 缓存。 */
    suspend fun getArticleListByCid(cid: String, page: Int): ArticleList

    /** 关键字搜索文章（page 从 0 开始） */
    suspend fun searchArticles(key: String, page: Int): ArticleList

    /** 我的收藏列表（page 从 0 开始） */
    suspend fun getCollectList(page: Int): ArticleList

    /** 带 SWR 缓存的 banner 流（约定仅首屏使用） */
    fun getBannerFlow(): Flow<CachedResult<BannerList>>

    /** 带 SWR 缓存的置顶文章流 */
    fun getArticleTopFlow(): Flow<CachedResult<TopArticle>>

    /** 带 SWR 缓存的首页文章列表流（约定仅首页 page 接入缓存） */
    fun getArticleListFlow(page: Int): Flow<CachedResult<ArticleList>>

    /** 带 SWR 缓存的体系文章列表流（约定仅首页 page 接入缓存） */
    fun getArticleListByCidFlow(cid: String, page: Int): Flow<CachedResult<ArticleList>>
}

/**
 * 项目相关接口。
 */
interface ProjectRepository {

    /** 项目列表（page 从 1 开始）。首页请改用 [getProjectListFlow] 走 SWR 缓存。 */
    suspend fun getProjectList(cid: String, page: Int): ArticleList

    /** 带 SWR 缓存的项目列表流（约定仅首页 page 接入缓存） */
    fun getProjectListFlow(cid: String, page: Int): Flow<CachedResult<ArticleList>>

    /** 带 SWR 缓存的项目分类树流 */
    fun getProjectTreeFlow(): Flow<CachedResult<ProjectTreeList>>
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
 * 导航 / 体系树 / 热搜词 等公共数据接口。
 */
interface CommonRepository {

    /** 积分排行榜（page 从 1 开始）。首页请改用 [getCoinRankFlow] 走 SWR 缓存。 */
    suspend fun getCoinRank(page: Int): CoinRank

    /** 带 SWR 缓存的导航流 */
    fun getNavigationFlow(): Flow<CachedResult<NavigationList>>

    /** 带 SWR 缓存的体系树流 */
    fun getSystemTreeFlow(): Flow<CachedResult<TreeList>>

    /** 带 SWR 缓存的热搜词流 */
    fun getHotKeyFlow(): Flow<CachedResult<HotKeyList>>

    /** 带 SWR 缓存的积分排行榜流（约定仅首页 page 接入缓存） */
    fun getCoinRankFlow(page: Int): Flow<CachedResult<CoinRank>>
}
