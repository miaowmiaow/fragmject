package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * KV 键值实体。
 *
 * 自增 [id] 为主键保证写入顺序，业务键 [key]（列名 `first`）单独建唯一索引，
 * 使 `@Insert(onConflict = REPLACE)` 在相同 key 上真正触发 conflict 并原子覆盖，
 * 从根本上消除 find-then-write 的并发竞态。
 */
@Entity(
    tableName = "kv_table",
    indices = [Index(value = ["first"], unique = true)]
)
data class KVEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "first") val key: String,
    @ColumnInfo(name = "second") var value: String?
)
