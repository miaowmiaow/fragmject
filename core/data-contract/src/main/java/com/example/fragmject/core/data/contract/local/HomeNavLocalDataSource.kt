package com.example.fragmject.core.data.contract.local

import com.example.fragmject.core.model.NavTab
import kotlinx.coroutines.flow.Flow

/**
 * 首页导航 tab 本地数据源。
 *
 * 由 core:database 使用 Room DAO/Entity/Mapping 实现；core:data 只依赖本契约。
 */
interface HomeNavLocalDataSource {
    fun observeNavigation(): Flow<List<NavTab>>
    suspend fun saveNavigation(items: List<NavTab>)
}
