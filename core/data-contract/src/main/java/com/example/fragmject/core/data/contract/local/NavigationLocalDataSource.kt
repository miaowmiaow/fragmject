package com.example.fragmject.core.data.contract.local

import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow

/**
 * 导航 / 体系树本地数据源。
 *
 * 由 core:database 使用 Room DAO/Entity/Mapping 实现；core:data 只依赖本契约。
 */
interface NavigationLocalDataSource {
    fun observeNavigation(): Flow<List<Navigation>>
    fun observeSystemTree(): Flow<List<Tree>>
    suspend fun saveNavigation(items: List<Navigation>)
    suspend fun saveSystemTree(items: List<Tree>)
}
