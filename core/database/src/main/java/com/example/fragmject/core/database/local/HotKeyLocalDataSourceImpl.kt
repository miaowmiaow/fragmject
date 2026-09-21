package com.example.fragmject.core.database.local

import com.example.fragmject.core.database.dao.HotKeyDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.contract.local.HotKeyLocalDataSource
import com.example.fragmject.core.model.HotKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HotKeyLocalDataSource] 的 Room 适配器实现。
 */
@Singleton
class HotKeyLocalDataSourceImpl @Inject constructor(
    private val hotKeyDao: HotKeyDao,
) : HotKeyLocalDataSource {

    private companion object {
        const val KEY_HOTKEY = "hotkey"
    }

    override fun observeHotKey(): Flow<List<HotKey>> =
        hotKeyDao.getByCacheKey(KEY_HOTKEY).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveHotKey(items: List<HotKey>) {
        hotKeyDao.replaceAll(
            KEY_HOTKEY,
            items.mapIndexed { i, hk -> hk.toEntity(KEY_HOTKEY, i) },
        )
    }
}
