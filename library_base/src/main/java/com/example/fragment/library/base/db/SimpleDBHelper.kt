package com.example.fragment.library.base.db

import androidx.room.*
import com.example.fragment.library.base.component.provider.BaseProvider

@Database(entities = [SimpleDBHelper.KVEntity::class], version = 1, exportSchema = false)
abstract class SimpleDBHelper : RoomDatabase() {

    companion object {

        @Volatile
        private var database: SimpleDBHelper? = null

        private fun getDatabase() = database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                BaseProvider.mContext.applicationContext,
                SimpleDBHelper::class.java,
                SimpleDBHelper::class.java.simpleName
            ).allowMainThreadQueries()
                .build().also { db ->
                    database = db
                }
        }

        fun set(key: String, value: String) {
            getDatabase().set(key, value)
        }

        fun get(key: String): String? {
            return getDatabase().get(key)
        }
    }

    abstract fun getDao(): KVDao

    @Synchronized
    fun set(key: String, value: String) {
        getDatabase().also { db ->
            val dao = db.getDao()
            val kv = dao.findByKey(key)
            if (kv == null) {
                dao.insert(KVEntity(key = key, value = value))
            } else {
                kv.value = value
                dao.update(kv)
            }
            db.closeDatabase() //关闭策略待优化!!!
        }
    }

    @Synchronized
    fun get(key: String): String? {
        return getDatabase().let { db ->
            val dao = db.getDao()
            val value = dao.findByKey(key)?.value
            db.closeDatabase() //关闭策略待优化!!!
            value
        }
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
