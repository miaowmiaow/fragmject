package com.example.fragmject.core.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.impl.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.impl.paging.ProjectPagingSource
import com.example.fragmject.core.data.contract.local.ProjectTreeLocalDataSource
import com.example.fragmject.core.data.impl.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.data.contract.remote.ProjectRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ProjectRepository] 领域端口的 data 层适配器。
 *
 * 项目树（Tab 栏）保留 Room 缓存；文章列表走纯网络分页。
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectTreeLocal: ProjectTreeLocalDataSource,
    private val remote: ProjectRemoteDataSource,
) : ProjectRepository {

    override fun observeProjectTree(): Flow<List<ProjectTree>> = projectTreeLocal.observeProjectTree()

    override suspend fun refreshProjectTree(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { remote.fetchProjectTree() },
        ) { resp ->
            resp.data?.let { data ->
                projectTreeLocal.saveProjectTree(data)
            } ?: Unit
        }
    }

    override fun getProjectPagingData(cid: String): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { ProjectPagingSource(cid, remote) },
    ).flow
}
