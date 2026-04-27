package com.example.fragment.project.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
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