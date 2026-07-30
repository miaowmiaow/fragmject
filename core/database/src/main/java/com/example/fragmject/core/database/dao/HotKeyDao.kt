package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragmject.core.database.model.HotKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HotKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hotKeys: List<HotKeyEntity>)

    @Transaction
    suspend fun replaceAll(cacheKey: String, entities: List<HotKeyEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    @Query("SELECT * FROM hot_key WHERE cache_key = :cacheKey ORDER BY sort_order ASC")
    fun getByCacheKey(cacheKey: String): Flow<List<HotKeyEntity>>

    @Query("DELETE FROM hot_key WHERE cache_key = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)
}
