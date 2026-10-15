package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.example.fragmject.core.database.model.OtpAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpAccountDao {

    @Query("SELECT * FROM otp_account ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<OtpAccountEntity>>

    @Query("SELECT * FROM otp_account WHERE id = :id")
    suspend fun getById(id: Long): OtpAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OtpAccountEntity): Long

    @Update
    suspend fun update(entity: OtpAccountEntity)

    @Query("DELETE FROM otp_account WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 当前最大排序值；空表返回 -1，便于调用方直接 +1 得到新序号。 */
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM otp_account")
    suspend fun maxSortOrder(): Int
}
