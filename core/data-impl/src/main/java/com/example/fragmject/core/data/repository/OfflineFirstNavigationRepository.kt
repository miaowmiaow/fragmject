package com.example.fragmject.core.data.impl.repository

import com.example.fragmject.core.data.contract.local.NavigationLocalDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.impl.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 唯一数据源的导航 / 体系树 Repository。
 *
 * 缓存细节由 [NavigationLocalDataSource] 承载，本适配器只负责离线优先编排。
 */
@Singleton
class OfflineFirstNavigationRepository @Inject constructor(
    private val navigationLocal: NavigationLocalDataSource,
    private val commonRepo: CommonRemoteDataSource,
) : NavigationRepository {

    override fun observeNavigation(): Flow<List<Navigation>> = navigationLocal.observeNavigation()

    override fun observeSystemTree(): Flow<List<Tree>> = navigationLocal.observeSystemTree()

    override suspend fun refreshNavigation(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonRepo.fetchNavigation() },
        ) { resp ->
            resp.data?.let { data ->
                navigationLocal.saveNavigation(data)
            } ?: Unit
        }
    }

    override suspend fun refreshSystemTree(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonRepo.fetchSystemTree() },
        ) { resp ->
            resp.data?.let { data ->
                navigationLocal.saveSystemTree(data)
            } ?: Unit
        }
    }
}
