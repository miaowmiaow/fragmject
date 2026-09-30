package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.HomeNavLocalDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.repository.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.HomeNavRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.NavTab
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 唯一数据源的首页导航 tab Repository。
 *
 * 缓存细节由 [HomeNavLocalDataSource] 承载，本适配器只负责离线优先编排。
 */
@Singleton
class HomeNavRepositoryImpl @Inject constructor(
    private val homeNavLocal: HomeNavLocalDataSource,
    private val commonRepo: CommonRemoteDataSource,
) : HomeNavRepository {

    override fun observeNavigation(): Flow<List<NavTab>> = homeNavLocal.observeNavigation()

    override suspend fun refreshNavigation(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonRepo.fetchNavigation() },
        ) { resp ->
            resp.data?.let { data ->
                homeNavLocal.saveNavigation(data)
            } ?: Unit
        }
    }
}
