package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.example.fragmject.core.database.model.KVEntity

@Dao
interface KVDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kv: KVEntity): Long

    @Update
    suspend fun update(kv: KVEntity): Int

    @Query("SELECT * FROM kv_table WHERE first = :key ORDER BY id DESC LIMIT 1")
    suspend fun findByKey(key: String): KVEntity?

}