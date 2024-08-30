package com.example.fragment.project.database.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History): Long

    @Delete
    suspend fun delete(history: History): Int

    @Query("SELECT * FROM history WHERE `key` = :key And `value` = :value ORDER BY id DESC LIMIT 1")
    suspend fun getByValue(key: String, value: String): History?

    @Query("SELECT * FROM history WHERE `key` = :key ORDER BY id DESC")
    fun getByType(key: String): Flow<List<History>>

}