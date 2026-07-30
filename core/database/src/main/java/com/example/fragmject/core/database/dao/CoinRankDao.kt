package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragmject.core.database.model.CoinRankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinRankDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ranks: List<CoinRankEntity>)

    @Transaction
    suspend fun replaceAll(cacheKey: String, entities: List<CoinRankEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    @Query("SELECT * FROM coin_rank WHERE cache_key = :cacheKey ORDER BY sort_order ASC")
    fun getByCacheKey(cacheKey: String): Flow<List<CoinRankEntity>>

    @Query("DELETE FROM coin_rank WHERE cache_key = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)

    /** 按前缀批量删除（刷新首页时清除所有旧页数据）。 */
    @Query("DELETE FROM coin_rank WHERE cache_key LIKE :prefix")
    suspend fun deleteByPrefix(prefix: String)

    @Query("SELECT * FROM coin_rank WHERE cache_key LIKE :prefix ORDER BY sort_order ASC")
    fun getByPagePrefix(prefix: String): Flow<List<CoinRankEntity>>
}