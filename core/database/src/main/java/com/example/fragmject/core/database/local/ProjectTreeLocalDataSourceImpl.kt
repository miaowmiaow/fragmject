package com.example.fragmject.core.database.local

import com.example.fragmject.core.database.dao.ProjectTreeDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.contract.local.ProjectTreeLocalDataSource
import com.example.fragmject.core.model.ProjectTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ProjectTreeLocalDataSource] 的 Room 适配器实现。
 */
@Singleton
class ProjectTreeLocalDataSourceImpl @Inject constructor(
    private val projectTreeDao: ProjectTreeDao,
) : ProjectTreeLocalDataSource {

    private companion object {
        const val KEY_TREE = "project_tree"
    }

    override fun observeProjectTree(): Flow<List<ProjectTree>> =
        projectTreeDao.getByCacheKey(KEY_TREE).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveProjectTree(items: List<ProjectTree>) {
        projectTreeDao.replaceAll(
            KEY_TREE,
            items.mapIndexed { i, pt -> pt.toEntity(KEY_TREE, i) },
        )
    }
}
