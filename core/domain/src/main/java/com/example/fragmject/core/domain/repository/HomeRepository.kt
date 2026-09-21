package com.example.fragmject.core.domain.repository

import androidx.paging.PagingData
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import kotlinx.coroutines.flow.Flow

/**
 * 首页领域端口：文章列表纯网络分页 + banner/top 头部网络拉取。
 *
 * banner/top 拉取返回 [DomainResult]，由调用方（UseCase/ViewModel）显式决定降级策略；
 * 数据层不得无记录地把网络失败静默转换为空集合。
 */
interface HomeRepository {
    /** 首页文章分页数据流（page 从 0 开始，纯网络）。 */
    fun getHomePagingData(): Flow<PagingData<Article>>

    /** 拉取首页 banner（轮播图）。 */
    suspend fun fetchHomeBanners(): DomainResult<List<Banner>>

    /** 拉取首页置顶文章。 */
    suspend fun fetchTopArticles(): DomainResult<List<Article>>
}
