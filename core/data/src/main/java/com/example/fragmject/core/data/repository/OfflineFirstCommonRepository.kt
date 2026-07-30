package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room 唯一数据源的公共数据 Repository。
 *
 * 缓存键约定（单页数据，无分页）：
 * - "nav"     → 导航
 * - "tree"    → 体系树
 * - "hotkey"  → 热搜词
 */
class OfflineFirstCommonRepository(
    private val commonRepo: CommonRepository,
) {

    // ===== 观察 Room =====

    fun observeNavigation(): Flow<List<Navigation>> =
        AppDatabase.getNavigationDao().getByCacheKey("nav").map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeSystemTree(): Flow<List<Tree>> =
        AppDatabase.getTreeDao().getByCacheKey("tree").map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeHotKey(): Flow<List<HotKey>> =
        AppDatabase.getHotKeyDao().getByCacheKey("hotkey").map { entities ->
            entities.map { it.toDomain() }
        }

    // ===== 网络 → Room 写入 =====

    suspend fun refreshAll() {
        refreshNavigation()
        refreshSystemTree()
        refreshHotKey()
    }

    suspend fun refreshNavigation() {
        val result = runCatching { commonRepo.fetchNavigation() }
        val data = result.getOrNull()?.data ?: return
        AppDatabase.getNavigationDao().replaceAll("nav",
            data.mapIndexed { i, nav -> nav.toEntity("nav", i) })
    }

    suspend fun refreshSystemTree() {
        val result = runCatching { commonRepo.fetchSystemTree() }
        val data = result.getOrNull()?.data ?: return
        AppDatabase.getTreeDao().replaceAll("tree",
            data.mapIndexed { i, tree -> tree.toEntity("tree", i) })
    }

    suspend fun refreshHotKey() {
        val result = runCatching { commonRepo.fetchHotKey() }
        val data = result.getOrNull()?.data ?: return
        AppDatabase.getHotKeyDao().replaceAll("hotkey",
            data.mapIndexed { i, hk -> hk.toEntity("hotkey", i) })
    }
}