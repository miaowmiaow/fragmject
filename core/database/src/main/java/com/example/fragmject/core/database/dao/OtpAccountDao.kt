package com.example.fragmject.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.example.fragmject.core.database.model.OtpAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpAccountDao {

    @Query("SELECT * FROM otp_account ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<OtpAccountEntity>>

    @Query("SELECT * FROM otp_account WHERE id = :id")
    suspend fun getById(id: Long): OtpAccountEntity?

    /** 单条插入也走列表版本（`insertAllAppendingSort`），避免两条插入路径的序号逻辑分叉。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OtpAccountEntity>): List<Long>

    /**
     * 批量新增，并由本方法统一续接 `sort_order`（传进来的值会被覆盖）。
     *
     * 整体置于 `@Transaction`：`maxSortOrder()` 的读与 insert 的写必须原子，
     * 否则并发写入或循环调用会拿到同一批序号；任一条插入失败时整批回滚，
     * 不会出现「导入了一半」的中间状态。
     */
    @Transaction
    suspend fun insertAllAppendingSort(entities: List<OtpAccountEntity>): List<Long> {
        val base = maxSortOrder() + 1
        return insertAll(
            entities.mapIndexed { index, entity -> entity.copy(sortOrder = base + index) },
        )
    }

    @Update
    suspend fun update(entity: OtpAccountEntity)

    @Query("DELETE FROM otp_account WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 当前最大排序值；空表返回 -1，便于调用方直接 +1 得到新序号。 */
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM otp_account")
    suspend fun maxSortOrder(): Int
}
