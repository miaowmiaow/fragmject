package com.example.fragment.library.base.db

import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.example.fragment.library.base.component.provider.BaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * RoomDatabase的简单封装
 *      存值：SimpleDBHelper.set(key: String, value: String)
 *      取值：SimpleDBHelper.get(key: String)
 * 详细使用方法参考WanHelper.kt
 */
@Database(entities = [SimpleDBHelper.KVEntity::class], version = 1, exportSchema = false)
abstract class SimpleDBHelper : RoomDatabase() {

    companion object {

        @Volatile
        private var database: SimpleDBHelper? = null

        private fun getDatabase() = database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                BaseProvider.mContext.applicationContext,
                SimpleDBHelper::class.java,
                "SimpleDatabase"
            ).build().also { db ->
                database = db
            }
        }

        fun set(key: String, value: String) {
            getDatabase().set(key, value)
        }

        fun get(key: String): MutableLiveData<String> {
            return getDatabase().get(key)
        }

    }

    abstract fun getDao(): KVDao

    @Synchronized
    fun set(key: String, value: String) {
        CoroutineScope(Dispatchers.IO).launch {
            database?.apply {
                try {
                    val dao = getDao()
                    val kv = dao.findByKey(key)
                    if (kv == null) {
                        dao.insert(KVEntity(key = key, value = value))
                    } else {
                        kv.value = value
                        dao.update(kv)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    closeDatabase() //关闭策略待优化!!!
                }
            }
        }
    }

    @Synchronized
    fun get(key: String): MutableLiveData<String> {
        val value = MutableLiveData<String>()
        CoroutineScope(Dispatchers.IO).launch {
            database?.apply {
                try {
                    val dao = getDao()
                    value.postValue(dao.findByKey(key)?.value)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    closeDatabase() //关闭策略待优化!!!
                }
            }
        }
        return value
    }

    @Synchronized
    fun closeDatabase() {
        if (isOpen) {
            close()
        }
        database = null
    }

    @Dao
    interface KVDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(kv: KVEntity): Long

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(vararg kvs: KVEntity): Array<Long>

        @Update
        suspend fun update(vararg kv: KVEntity): Int

        @Query("SELECT * FROM kv WHERE `key` = :key ORDER BY id DESC LIMIT 1")
        suspend fun findByKey(key: String): KVEntity?

        @Query("SELECT * FROM kv")
        suspend fun findAll(): Array<KVEntity>?

        @Delete
        suspend fun delete(kv: KVEntity): Int

        @Delete
        suspend fun deleteAll(vararg kvs: KVEntity): Int

    }

    @Entity(tableName = "kv")
    data class KVEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        @ColumnInfo(name = "key")
        var key: String,
        @ColumnInfo(name = "value")
        var value: String
    )
}
