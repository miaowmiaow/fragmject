package com.example.fragment.project.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.fragment.project.data.History
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History): Long

    @Query("SELECT * FROM history WHERE `key` = :key ORDER BY id DESC")
    fun getByKey(key: String): Flow<List<History>>

    @Query("SELECT * FROM history WHERE `key` = :key And `value` = :value ORDER BY id DESC LIMIT 1")
    suspend fun getByValue(key: String, value: String): History?

    @Query("SELECT * FROM history WHERE `key` = :key And `url` = :url ORDER BY id DESC LIMIT 1")
    suspend fun getByUrl(key: String, url: String): History?

    @Query("SELECT COUNT(*) FROM History")
    suspend fun getCount(): Int

    @Query("DELETE FROM History WHERE id IN (SELECT id FROM History ORDER BY id ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Delete
    suspend fun delete(history: History): Int

    @Transaction
    suspend fun insertWithLimitCheck(history: History) {
        insert(history)
        val count = getCount()
        if (count > 10000) {
            val excessCount = count - 10000
            deleteOldest(excessCount)
        }
    }
}