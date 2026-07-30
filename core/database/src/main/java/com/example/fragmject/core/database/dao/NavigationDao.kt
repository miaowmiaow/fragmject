package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragmject.core.database.model.NavigationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NavigationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(navs: List<NavigationEntity>)

    @Transaction
    suspend fun replaceAll(cacheKey: String, entities: List<NavigationEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    @Query("SELECT * FROM navigation WHERE cache_key = :cacheKey ORDER BY sort_order ASC")
    fun getByCacheKey(cacheKey: String): Flow<List<NavigationEntity>>

    @Query("DELETE FROM navigation WHERE cache_key = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)
}
