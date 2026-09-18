package com.example.fragmject.core.data.repository

import com.example.fragmject.core.network.datasource.CommonDataSource

import com.example.fragmject.core.database.dao.NavigationDao
import com.example.fragmject.core.database.dao.TreeDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 唯一数据源的导航 / 体系树 Repository。
 *
 * 缓存键约定（单页数据，无分页）：
 * - "nav"  → 导航
 * - "tree" → 体系树
 */
@Singleton
class OfflineFirstNavigationRepository @Inject constructor(
    private val navigationDao: NavigationDao,
    private val treeDao: TreeDao,
    private val commonRepo: CommonDataSource,
) : NavigationRepository {

    override fun observeNavigation(): Flow<List<Navigation>> =
        navigationDao.getByCacheKey("nav").map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeSystemTree(): Flow<List<Tree>> =
        treeDao.getByCacheKey("tree").map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshNavigation(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonRepo.fetchNavigation() },
        ) { resp ->
            resp.data?.let { data ->
                navigationDao.replaceAll(
                    "nav",
                    data.mapIndexed { i, nav -> nav.toEntity("nav", i) })
            } ?: Unit
        }
    }

    override suspend fun refreshSystemTree(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonRepo.fetchSystemTree() },
        ) { resp ->
            resp.data?.let { data ->
                treeDao.replaceAll(
                    "tree",
                    data.mapIndexed { i, tree -> tree.toEntity("tree", i) })
            } ?: Unit
        }
    }
}
