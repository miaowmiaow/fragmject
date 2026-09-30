package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.NavTab
import kotlinx.coroutines.flow.Flow

/**
 * 首页导航 tab 领域端口（Room 唯一数据源模式）。
 */
interface HomeNavRepository {
    fun observeNavigation(): Flow<List<NavTab>>
    suspend fun refreshNavigation(): DomainResult<Unit>
}
