package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragmject.core.database.model.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    /** 批量插入或替换（按 cache_key + articleId 唯一索引去重）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    /** 原子替换：先删后写，对 Flow 订阅方仅 emit 最终结果，避免空列表闪烁。 */
    @Transaction
    suspend fun replaceAll(cacheKey: String, entities: List<ArticleEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    /** 观察指定 cacheKey 下的所有文章，按 sortOrder 排序。 */
    @Query("SELECT * FROM article WHERE cache_key = :cacheKey ORDER BY sort_order ASC")
    fun getByCacheKey(cacheKey: String): Flow<List<ArticleEntity>>

    /** 删除指定 cacheKey 下的所有缓存（刷新时先删再写，保证一致性）。 */
    @Query("DELETE FROM article WHERE cache_key = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)

    /** 按前缀批量删除（刷新首页时清除所有旧页数据）。 */
    @Query("DELETE FROM article WHERE cache_key LIKE :prefix")
    suspend fun deleteByPrefix(prefix: String)

    /** 清理超过 [threshold] 毫秒的旧缓存。 */
    @Query("DELETE FROM article WHERE timestamp < :threshold")
    suspend fun cleanExpired(threshold: Long)

    /** 统计指定 cacheKey 下的文章数（分页追加时用于计算 sortOrder 偏移）。 */
    @Query("SELECT COUNT(*) FROM article WHERE cache_key = :cacheKey")
    suspend fun countByCacheKey(cacheKey: String): Int

    /** 按前缀模糊匹配 cacheKey（用于合并多页数据）。 */
    @Query("SELECT * FROM article WHERE cache_key LIKE :prefix ORDER BY cache_key ASC, sort_order ASC")
    fun getByPagePrefix(prefix: String): Flow<List<ArticleEntity>>
}