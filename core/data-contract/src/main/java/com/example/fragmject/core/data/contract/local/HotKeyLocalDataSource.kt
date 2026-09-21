package com.example.fragmject.core.data.contract.local

import com.example.fragmject.core.model.HotKey
import kotlinx.coroutines.flow.Flow

/**
 * 热搜词本地数据源。
 *
 * 由 core:database 使用 Room DAO/Entity/Mapping 实现；core:data 只依赖本契约。
 */
interface HotKeyLocalDataSource {
    fun observeHotKey(): Flow<List<HotKey>>
    suspend fun saveHotKey(items: List<HotKey>)
}
