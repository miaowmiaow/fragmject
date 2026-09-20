package com.example.fragmject.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.paging.ProjectPagingSource
import com.example.fragmject.core.database.dao.ProjectTreeDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.network.datasource.ProjectDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ProjectRepository] 领域端口的 data 层适配器。
 *
 * 项目树（Tab 栏）保留 Room 缓存；文章列表走纯网络分页。
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectTreeDao: ProjectTreeDao,
    private val remote: ProjectDataSource,
) : ProjectRepository {

    companion object {
        const val CACHE_KEY_TREE = "project_tree"
    }

    override fun observeProjectTree(): Flow<List<ProjectTree>> =
        projectTreeDao.getByCacheKey(CACHE_KEY_TREE).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshProjectTree(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { remote.fetchProjectTree() },
        ) { resp ->
            resp.data?.let { data ->
                projectTreeDao.replaceAll(
                    CACHE_KEY_TREE,
                    data.mapIndexed { i, pt -> pt.toEntity(CACHE_KEY_TREE, i) })
            } ?: Unit
        }
    }

    override fun getProjectPagingData(cid: String): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { ProjectPagingSource(cid, remote) },
    ).flow
}
