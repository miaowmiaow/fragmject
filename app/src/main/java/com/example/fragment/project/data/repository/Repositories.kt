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

/**
 * 文章相关接口：首页 / 体系 / 搜索 / 收藏。
 *
 * Repository 设计动机：
 * 1. 把网络细节（URL、参数、占位符）封装在 data 层，ViewModel 只关心业务方法名；
 * 2. 接口 + 默认实现，便于后续替换 mock / 单测；
 * 3. 不引入 DI 框架，使用极轻量的 [WanRepositoryProvider] 单例工厂。
 */
interface ArticleRepository {

    /** 首页 banner */
    suspend fun getBanner(): BannerList

    /** 首页置顶文章 */
    suspend fun getArticleTop(): TopArticle

    /** 首页文章列表，分页（page 从 0 开始） */
    suspend fun getArticleList(page: Int): ArticleList

    /** 知识体系下的文章列表（page 从 0 开始） */
    suspend fun getArticleListByCid(cid: String, page: Int): ArticleList

    /** 关键字搜索文章（page 从 0 开始） */
    suspend fun searchArticles(key: String, page: Int): ArticleList

    /** 我的收藏列表（page 从 0 开始） */
    suspend fun getCollectList(page: Int): ArticleList
}

/**
 * 项目相关接口。
 */
interface ProjectRepository {

    /** 项目列表（page 从 1 开始） */
    suspend fun getProjectList(cid: String, page: Int): ArticleList

    /** 项目分类树 */
    suspend fun getProjectTree(): ProjectTreeList
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

    suspend fun getNavigation(): NavigationList

    suspend fun getSystemTree(): TreeList

    suspend fun getHotKey(): HotKeyList

    /** 积分排行榜（page 从 1 开始） */
    suspend fun getCoinRank(page: Int): CoinRank
}
