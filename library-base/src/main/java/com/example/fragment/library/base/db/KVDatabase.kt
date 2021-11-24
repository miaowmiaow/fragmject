package com.example.fragment.library.base.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.example.fragment.library.base.provider.BaseContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                BaseContent.get().applicationContext,
                KVDatabase::class.java,
                KVDatabase::class.java.simpleName
            ).build().also { db -> database = db }
        }

        fun set(key: String, value: String) {
            getDB().set(key, value)
        }

        fun get(key: String): LiveData<String> {
            return getDB().get(key)
        }

        fun closeDatabase(){
            getDB().close()
        }

    }

    abstract fun getDao(): KVDao

    fun set(key: String, value: String) {
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
            } finally {
                //用完关闭会影响性能，此处待优化
//                if (database != null) {
//                    close()
//                }
            }
        }
    }

    fun get(key: String): MutableLiveData<String> {
        val result = MutableLiveData<String>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                getDao().findByKey(key)?.value?.let {
                    result.postValue(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
//                if (database != null) {
//                    close()
//                }
            }
        }
        return result
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

    @Query("SELECT * FROM kv_table WHERE `key` = :key ORDER BY id DESC LIMIT 1")
    suspend fun findByKey(key: String): KV?

}

@Entity(tableName = "kv_table")
data class KV(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "key")
    var key: String,

    @ColumnInfo(name = "value")
    var value: String

)
