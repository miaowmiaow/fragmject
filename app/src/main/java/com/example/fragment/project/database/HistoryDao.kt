package com.example.fragment.project.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.fragment.project.data.History
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    companion object {
        private const val HISTORY_LIMIT = 10000
        private const val TRIM_CHECK_INTERVAL = 50L
    }

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

    @Query("DELETE FROM History WHERE id NOT IN (SELECT id FROM History ORDER BY id DESC LIMIT :limit)")
    suspend fun trimToLimit(limit: Int): Int

    @Delete
    suspend fun delete(history: History): Int

    @Transaction
    suspend fun insertWithLimitCheck(history: History) {
        val insertedId = insert(history)
        if (insertedId % TRIM_CHECK_INTERVAL == 0L) {
            trimToLimit(HISTORY_LIMIT)
        }
    }
}