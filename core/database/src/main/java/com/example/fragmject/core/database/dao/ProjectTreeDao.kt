package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragmject.core.database.model.ProjectTreeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectTreeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trees: List<ProjectTreeEntity>)

    @Transaction
    suspend fun replaceAll(cacheKey: String, entities: List<ProjectTreeEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    @Query("SELECT * FROM project_tree WHERE cache_key = :cacheKey ORDER BY sort_order ASC")
    fun getByCacheKey(cacheKey: String): Flow<List<ProjectTreeEntity>>

    @Query("DELETE FROM project_tree WHERE cache_key = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)
}
