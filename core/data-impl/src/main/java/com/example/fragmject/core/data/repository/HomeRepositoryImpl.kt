package com.example.fragmject.core.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.impl.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.impl.paging.HomePagingSource
import com.example.fragmject.core.data.impl.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HomeRepository] 领域端口的 data 层适配器。
 *
 * 纯网络：文章列表走 [HomePagingSource]，banner/top 头部直接网络拉取。
 * banner/top 通过 [fetchAsDomainResult] 显式返回 [DomainResult]，
 * 网络失败不再被静默吞掉，交由调用方决定降级策略。
 */
@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remote: ArticleRemoteDataSource,
) : HomeRepository {

    override fun getHomePagingData(): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { HomePagingSource(remote) },
    ).flow

    override suspend fun fetchHomeBanners(): DomainResult<List<Banner>> =
        fetchAsDomainResult(call = { remote.fetchBannerList() }) { it.data.orEmpty() }

    override suspend fun fetchTopArticles(): DomainResult<List<Article>> =
        fetchAsDomainResult(call = { remote.fetchArticleTop() }) { it.data.orEmpty() }
}
