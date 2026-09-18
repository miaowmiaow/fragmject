package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow

/**
 * 导航 / 体系树领域端口（Room 唯一数据源模式）。
 */
interface NavigationRepository {
    fun observeNavigation(): Flow<List<Navigation>>
    fun observeSystemTree(): Flow<List<Tree>>
    suspend fun refreshNavigation(): DomainResult<Unit>
    suspend fun refreshSystemTree(): DomainResult<Unit>
}
