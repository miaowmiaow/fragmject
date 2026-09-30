package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.contract.local.SystemTreeLocalDataSource
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.repository.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.repository.paging.SystemPagingSource
import com.example.fragmject.core.data.repository.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SystemRepository] 领域端口的 data 层适配器。
 *
 * 体系树走 Room 唯一数据源（离线优先），体系文章纯网络分页。
 */
@Singleton
class SystemRepositoryImpl @Inject constructor(
    private val remote: ArticleRemoteDataSource,
    private val systemTreeLocal: SystemTreeLocalDataSource,
    private val commonRepo: CommonRemoteDataSource,
) : SystemRepository {

    override fun observeSystemTree(): Flow<List<Tree>> = systemTreeLocal.observeSystemTree()

    override suspend fun refreshSystemTree(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonRepo.fetchSystemTree() },
        ) { resp ->
            resp.data?.let { data ->
                systemTreeLocal.saveSystemTree(data)
            } ?: Unit
        }
    }

    override fun getSystemPagingData(cid: String): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { SystemPagingSource(cid, remote) },
    ).flow
}
