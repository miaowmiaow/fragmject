package com.example.fragment.project.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.example.fragment.project.data.History
import com.example.fragment.project.data.User
import com.example.miaow.base.provider.BaseContentProvider

@Database(entities = [History::class, User::class], version = 6, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun userDao(): UserDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(connection, "CREATE INDEX IF NOT EXISTS `index_History_key_id` ON `History` (`key`, `id`)")
                executeSql(connection, "CREATE INDEX IF NOT EXISTS `index_History_key_value_id` ON `History` (`key`, `value`, `id`)")
                executeSql(connection, "CREATE INDEX IF NOT EXISTS `index_History_key_url_id` ON `History` (`key`, `url`, `id`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(connection, "ALTER TABLE `User` DROP COLUMN `dark_theme`")
            }
        }

        private fun executeSql(connection: SQLiteConnection, sql: String) {
            val statement = connection.prepare(sql)
            statement.use { statement ->
                statement.step()
            }
        }

        private fun getDatabase() = INSTANCE ?: synchronized(AppDatabase::class.java) {
            INSTANCE ?: buildDatabase().also {
                INSTANCE = it
            }
        }

        private fun buildDatabase(context: Context = BaseContentProvider.context()): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
        }

        @JvmStatic
        fun getHistoryDao(): HistoryDao {
            return getDatabase().historyDao()
        }

        @JvmStatic
        fun getUserDao(): UserDao {
            return getDatabase().userDao()
        }

    }

    override fun close() {
        super.close()
        //数据库关闭后把instance置空
        INSTANCE = null
    }

}