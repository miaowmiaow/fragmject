package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragmject.core.database.model.TreeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trees: List<TreeEntity>)

    @Transaction
    suspend fun replaceAll(cacheKey: String, entities: List<TreeEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    @Query("SELECT * FROM tree WHERE cache_key = :cacheKey ORDER BY sort_order ASC")
    fun getByCacheKey(cacheKey: String): Flow<List<TreeEntity>>

    @Query("DELETE FROM tree WHERE cache_key = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)
}
