package com.example.fragment.library.base.utils

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
@Database(entities = [SimpleEntity::class], version = 1, exportSchema = false)
abstract class DBHelper : RoomDatabase() {

    companion object {

        @Volatile
        private var database: DBHelper? = null

        private fun getDatabase() = database ?: synchronized(DBHelper::class.java) {
            database ?: Room.databaseBuilder(
                BaseContent.get().applicationContext,
                DBHelper::class.java,
                DBHelper::class.java.simpleName
            ).build().also { db -> database = db }
        }

        fun set(key: String, value: String?, expire: Long = Long.MAX_VALUE) {
            getDatabase().set(key, value, expire)
        }

        fun get(key: String): MutableLiveData<String> {
            return getDatabase().get(key)
        }

        fun closeDatabase(){
            getDatabase().closeDatabase()
        }
    }

    abstract fun getSimpleDao(): SimpleDao

    @Synchronized
    fun set(key: String, value: String?, expire: Long = Long.MAX_VALUE) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = getSimpleDao()
                var entity = dao.findByKey(key)
                if (entity == null) {
                    entity = SimpleEntity(key = key, value = value, expire = expire)
                    dao.insert(entity)
                } else {
                    entity.value = value
                    entity.expire = expire
                    dao.update(entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                closeDatabase()
            }
        }
    }

    @Synchronized
    fun get(key: String): MutableLiveData<String> {
        val result = MutableLiveData<String>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = getSimpleDao()
                dao.find()?.forEach {
                    if (it.expire < System.currentTimeMillis()) {
                        dao.delete(it)
                    }
                }
                val value = dao.findByKey(key)?.value
                result.postValue(value)
            } catch (e: Exception) {
                e.printStackTrace()
                closeDatabase()
            }
        }
        return result
    }

    @Synchronized
    fun closeDatabase() {
        if (isOpen) {
            close()
            database = null
        }
    }

}

@Dao
interface SimpleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SimpleEntity): Long

    @Delete
    suspend fun delete(entity: SimpleEntity): Int

    @Update
    suspend fun update(entity: SimpleEntity): Int

    @Query("SELECT * FROM simple_table WHERE `key` = :key ORDER BY id DESC LIMIT 1")
    suspend fun findByKey(key: String): SimpleEntity?

    @Query("SELECT * FROM simple_table")
    suspend fun find(): Array<SimpleEntity>?

}

@Entity(tableName = "simple_table")
data class SimpleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "key") var key: String,
    @ColumnInfo(name = "value") var value: String?,
    @ColumnInfo(name = "expire") var expire: Long = Long.MAX_VALUE
)
