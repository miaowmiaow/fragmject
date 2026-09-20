package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.paging.HomePagingSource
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.network.datasource.ArticleDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HomeRepository] 领域端口的 data 层适配器。
 *
 * 纯网络：文章列表走 [HomePagingSource]，banner/top 头部直接网络拉取。
 */
@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remote: ArticleDataSource,
) : HomeRepository {

    override fun getHomePagingData(): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { HomePagingSource(remote) },
    ).flow

    override suspend fun fetchHomeBanners(): List<Banner> =
        runCatching { remote.fetchBannerList().data.orEmpty() }.getOrDefault(emptyList())

    override suspend fun fetchTopArticles(): List<Article> =
        runCatching { remote.fetchArticleTop().data.orEmpty() }.getOrDefault(emptyList())
}
