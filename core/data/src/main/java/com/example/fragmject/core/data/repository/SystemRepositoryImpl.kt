package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.paging.SystemPagingSource
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.network.datasource.ArticleDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SystemRepository] 领域端口的 data 层适配器。
 *
 * 纯网络分页，直接透传 [ArticleDataSource.getArticleListByCid]。
 */
@Singleton
class SystemRepositoryImpl @Inject constructor(
    private val remote: ArticleDataSource,
) : SystemRepository {

    override fun getSystemPagingData(cid: String): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { SystemPagingSource(cid, remote) },
    ).flow
}
