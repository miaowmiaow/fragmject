package com.example.fragment.project.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fragment.project.data.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) //覆盖插入模式，根据主键值判断是否覆盖
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM user")
    fun get(): Flow<User?>

    @Delete
    suspend fun delete(user: User): Int
}