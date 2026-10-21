package com.example.fragmject.core.database

import android.content.Context
import android.util.Log
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import com.example.fragmject.core.database.dao.KVDao
import com.example.fragmject.core.database.model.KVEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 对 RoomDatabase 进行封装。
 * 具体用法见 core/database/local 下的各 LocalDataSource 实现（如 ScheduleLocalDataSourceImpl）。
 */
@Database(entities = [KVEntity::class], version = 2, exportSchema = true)
abstract class KVDatabase : RoomDatabase() {

    abstract fun kvDao(): KVDao

    companion object {

        /** v1→v2：为业务键 `first` 建立唯一索引，并清理历史重复数据。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                // 去重：每个 key 仅保留 id 最大的一条
                executeSql(
                    connection,
                    "DELETE FROM kv_table WHERE id NOT IN (SELECT MAX(id) FROM kv_table GROUP BY first)"
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_kv_table_first` ON `kv_table` (`first`)"
                )
            }
        }

        /** 生产环境注册的全部迁移，供 `KVDatabaseMigrationTest` 复用。 */
        internal val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        private fun executeSql(connection: SQLiteConnection, sql: String) {
            val statement = connection.prepare(sql)
            statement.use { statement ->
                statement.step()
            }
        }

        /** 构建数据库实例（由 Hilt DatabaseModule 调用并持有单例）。 */
        fun build(context: Context): KVDatabase {
            val builder = Room.databaseBuilder(
                context,
                KVDatabase::class.java,
                "kv_database"
            )
            // Room 只提供 vararg 重载，逐个注册以免 `*MIGRATIONS` 展开
            MIGRATIONS.forEach { builder.addMigrations(it) }
            return builder.build()
        }

    }

    suspend fun setValue(key: String, value: String): Boolean {
        return try {
            // 依赖 key 唯一索引 + REPLACE：相同 key 触发 conflict 原子覆盖（delete+insert），
            // 不再需要先 find 再 write 的分支，从根本上消除并发竞态。
            val id = kvDao().insert(KVEntity(id = 0, key = key, value = value))
            id > -1
        } catch (e: SQLiteException) {
            Log.e("KVDatabase", "setValue failed: key=$key", e)
            false
        }
    }

    suspend fun getValue(key: String): String {
        return try {
            kvDao().findByKey(key)?.value ?: ""
        } catch (e: SQLiteException) {
            Log.e("KVDatabase", "getValue failed: key=$key", e)
            ""
        }
    }

    fun observeValue(key: String): Flow<String> {
        return kvDao().observeByKey(key).map { it?.value ?: "" }
    }
}