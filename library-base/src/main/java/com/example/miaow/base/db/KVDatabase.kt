package com.example.miaow.base.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.miaow.base.provider.BaseContentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 对RoomDatabase进行封装
 * 详细使用方法参考WanHelper.kt
 */
@Database(entities = [KV::class], version = 1, exportSchema = false)
abstract class KVDatabase : RoomDatabase() {

    companion object {

        @Volatile
        private var database: KVDatabase? = null

        private fun getDB() = database ?: synchronized(KVDatabase::class.java) {
            database ?: Room.databaseBuilder(
                BaseContentProvider.get().applicationContext,
                KVDatabase::class.java,
                KVDatabase::class.java.simpleName
            ).build().also { db -> database = db }
        }

        @JvmStatic
        fun set(key: String, value: String) {
            getDB().setValue(key, value)
        }

        @JvmStatic
        suspend fun get(key: String): String {
            return getDB().getValue(key)
        }

        @JvmStatic
        fun get(key: String, result: (String) -> Unit) {
            getDB().getValue(key, result)
        }

        fun closeDatabase() {
            getDB().close()
        }

    }

    abstract fun getDao(): KVDao

    fun setValue(key: String, value: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var kv = getDao().findByKey(key)
                if (kv == null) {
                    kv = KV(key = key, value = value)
                    getDao().insert(kv)
                } else {
                    kv.value = value
                    getDao().update(kv)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getValue(key: String): String {
        return try {
            getDao().findByKey(key)?.value ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getValue(key: String, result: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val value = try {
                getDao().findByKey(key)?.value ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
            withContext(Dispatchers.Main) {
                result.invoke(value)
            }
        }
    }

    /**
     * 如果使用数据库频繁，则不建议每次操作后关闭
     */
    override fun close() {
        super.close()
        database = null
    }

}

@Dao
interface KVDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kv: KV): Long

    @Delete
    suspend fun delete(kv: KV): Int

    @Update
    suspend fun update(kv: KV): Int

    @Query("SELECT * FROM kv_table WHERE first = :key ORDER BY id DESC LIMIT 1")
    suspend fun findByKey(key: String): KV?

}

@Entity(tableName = "kv_table")
data class KV(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "first")
    val key: String,

    @ColumnInfo(name = "second")
    var value: String?

)
