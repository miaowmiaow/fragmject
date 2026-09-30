package com.example.fragmject.core.database.local

import com.example.fragmject.core.data.contract.local.HomeNavLocalDataSource
import com.example.fragmject.core.database.dao.HomeNavDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.NavTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HomeNavLocalDataSource] 的 Room 适配器实现。
 *
 * 首页导航 tab 走「单页缓存 + 整表替换」策略，对外只暴露领域模型。
 */
@Singleton
class HomeNavLocalDataSourceImpl @Inject constructor(
    private val homeNavDao: HomeNavDao,
) : HomeNavLocalDataSource {

    private companion object {
        const val KEY_NAV = "nav"
    }

    override fun observeNavigation(): Flow<List<NavTab>> =
        homeNavDao.getByCacheKey(KEY_NAV).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveNavigation(items: List<NavTab>) {
        homeNavDao.replaceAll(
            KEY_NAV,
            items.mapIndexed { i, nav -> nav.toEntity(KEY_NAV, i) },
        )
    }
}
