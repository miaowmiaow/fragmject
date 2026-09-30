package com.example.fragmject.core.database.local

import com.example.fragmject.core.data.contract.local.SystemTreeLocalDataSource
import com.example.fragmject.core.database.dao.TreeDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SystemTreeLocalDataSource] 的 Room 适配器实现。
 *
 * 体系树走「单页缓存 + 整表替换」策略，对外只暴露领域模型。
 */
@Singleton
class SystemTreeLocalDataSourceImpl @Inject constructor(
    private val treeDao: TreeDao,
) : SystemTreeLocalDataSource {

    private companion object {
        const val KEY_TREE = "tree"
    }

    override fun observeSystemTree(): Flow<List<Tree>> =
        treeDao.getByCacheKey(KEY_TREE).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveSystemTree(items: List<Tree>) {
        treeDao.replaceAll(
            KEY_TREE,
            items.mapIndexed { i, tree -> tree.toEntity(KEY_TREE, i) },
        )
    }
}
