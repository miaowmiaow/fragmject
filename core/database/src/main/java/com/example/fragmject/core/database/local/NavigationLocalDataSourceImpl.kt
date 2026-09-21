package com.example.fragmject.core.database.local

import com.example.fragmject.core.database.dao.NavigationDao
import com.example.fragmject.core.database.dao.TreeDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.contract.local.NavigationLocalDataSource
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NavigationLocalDataSource] 的 Room 适配器实现。
 *
 * 导航与体系树走「单页缓存 + 整表替换」策略，对外只暴露领域模型。
 */
@Singleton
class NavigationLocalDataSourceImpl @Inject constructor(
    private val navigationDao: NavigationDao,
    private val treeDao: TreeDao,
) : NavigationLocalDataSource {

    private companion object {
        const val KEY_NAV = "nav"
        const val KEY_TREE = "tree"
    }

    override fun observeNavigation(): Flow<List<Navigation>> =
        navigationDao.getByCacheKey(KEY_NAV).map { entities -> entities.map { it.toDomain() } }

    override fun observeSystemTree(): Flow<List<Tree>> =
        treeDao.getByCacheKey(KEY_TREE).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveNavigation(items: List<Navigation>) {
        navigationDao.replaceAll(
            KEY_NAV,
            items.mapIndexed { i, nav -> nav.toEntity(KEY_NAV, i) },
        )
    }

    override suspend fun saveSystemTree(items: List<Tree>) {
        treeDao.replaceAll(
            KEY_TREE,
            items.mapIndexed { i, tree -> tree.toEntity(KEY_TREE, i) },
        )
    }
}
