package com.example.miaow.base.database

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.miaow.base.provider.BaseContentProvider

/**
 * 对RoomDatabase进行封装
 * 详细使用方法参考WanHelper.kt
 */
@Database(entities = [KV::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kvDao(): KVDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        private fun getInstance() = instance ?: synchronized(AppDatabase::class.java) {
            instance ?: buildDatabase(BaseContentProvider.context()).also {
                instance = it
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "app-database"
            ).build()
        }

        @JvmStatic
        suspend fun set(key: String, value: String): Boolean {
            return getInstance().setValue(key, value)
        }

        @JvmStatic
        suspend fun get(key: String): String {
            return getInstance().getValue(key)
        }

        fun closeDB() {
            getInstance().close()
        }

    }

    suspend fun setValue(key: String, value: String): Boolean {
        return try {
            var kv = kvDao().findByKey(key)
            if (kv == null) {
                kv = KV(key = key, value = value)
                val id = kvDao().insert(kv) //返回 主键值 > -1 表示 insert 成功
                id > -1
            } else {
                kv.value = value
                val up = kvDao().update(kv) //返回 更新数 > 0表示 update 成功
                up > 0
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            false
        }
    }

    suspend fun getValue(key: String): String {
        return try {
            kvDao().findByKey(key)?.value ?: ""
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ""
        }
    }

    override fun close() {
        super.close()
        //数据库关闭后把instance置空
        instance = null
    }

}

@Dao
interface KVDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kv: KV): Long

    @Update
    suspend fun update(kv: KV): Int

    @Query("SELECT * FROM kv_table WHERE first = :key ORDER BY id DESC LIMIT 1")
    suspend fun findByKey(key: String): KV?

}

@Entity(tableName = "kv_table")
data class KV(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "first")
    val key: String,

    @ColumnInfo(name = "second")
    var value: String?

)
