package com.example.fragment.library.base.db

import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.example.fragment.library.base.component.provider.BaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            ).allowMainThreadQueries()
                .build().also { db ->
                    database = db
                }
        }

        fun setInMain(key: String, value: String) {
            getDatabase().setInMain(key, value)
        }
        
        fun set(key: String, value: String) {
            getDatabase().set(key, value)
        }

        fun getInMain(key: String): String? {
            return getDatabase().getInMain(key)
        }

        fun get(key: String): MutableLiveData<String> {
            return getDatabase().get(key)
        }
    }

    abstract fun getDao(): KVDao

    @Synchronized
    fun setInMain(key: String, value: String) {
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

    @Synchronized
    fun set(key: String, value: String) {
        CoroutineScope(Dispatchers.IO).launch {
            setInMain(key, value)
        }
    }

    @Synchronized
    fun getInMain(key: String): String? {
        database?.apply {
            return try {
                val dao = getDao()
                dao.findByKey(key)?.value
            } catch (e: Exception) {
                e.printStackTrace()
                e.message
            } finally {
                closeDatabase() //关闭策略待优化!!!
            }
        }
        return null
    }

    @Synchronized
    fun get(key: String): MutableLiveData<String> {
        val value = MutableLiveData<String>()
        CoroutineScope(Dispatchers.IO).launch {
            value.postValue(getInMain(key))
        }
        return value
    }

    @Synchronized
    private fun closeDatabase() {
        if (isOpen) {
            close()
        }
        database = null
    }


    @Dao
    interface KVDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(kv: KVEntity): Long

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertAll(vararg kvs: KVEntity): Array<Long>

        @Update
        fun update(vararg kv: KVEntity): Int

        @Query("SELECT * FROM kv WHERE `key` = :key ORDER BY id DESC LIMIT 1")
        fun findByKey(key: String): KVEntity?

        @Query("SELECT * FROM kv")
        fun findAll(): Array<KVEntity>?

        @Delete
        fun delete(kv: KVEntity): Int

        @Delete
        fun deleteAll(vararg kvs: KVEntity): Int

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
