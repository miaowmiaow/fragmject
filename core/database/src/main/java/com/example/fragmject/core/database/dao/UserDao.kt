package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.fragmject.core.database.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) //覆盖插入模式，根据主键值判断是否覆盖
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM user ORDER BY id DESC LIMIT 1")
    fun get(): Flow<UserEntity?>

    @Query("DELETE FROM user")
    suspend fun clear()

    @Delete
    suspend fun delete(user: UserEntity): Int
}