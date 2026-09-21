package com.example.fragmject.core.data.contract.local

import com.example.fragmject.core.model.ProjectTree
import kotlinx.coroutines.flow.Flow

/**
 * 项目树本地数据源。
 *
 * 由 core:database 使用 Room DAO/Entity/Mapping 实现；core:data 只依赖本契约。
 */
interface ProjectTreeLocalDataSource {
    fun observeProjectTree(): Flow<List<ProjectTree>>
    suspend fun saveProjectTree(items: List<ProjectTree>)
}
