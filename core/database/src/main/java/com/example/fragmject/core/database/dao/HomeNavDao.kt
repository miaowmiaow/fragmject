package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragmject.core.database.model.HomeNavEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeNavDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(navs: List<HomeNavEntity>)

    @Transaction
    suspend fun replaceAll(cacheKey: String, entities: List<HomeNavEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    @Query("SELECT * FROM home_nav WHERE cache_key = :cacheKey ORDER BY sort_order ASC")
    fun getByCacheKey(cacheKey: String): Flow<List<HomeNavEntity>>

    @Query("DELETE FROM home_nav WHERE cache_key = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)
}
