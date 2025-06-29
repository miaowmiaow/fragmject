package com.example.fragment.project.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fragment.project.data.History
import com.example.fragment.project.data.User
import com.example.miaow.base.provider.BaseContentProvider

@Database(entities = [History::class, User::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun userDao(): UserDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun getDatabase() = INSTANCE ?: synchronized(AppDatabase::class.java) {
            INSTANCE ?: buildDatabase().also {
                INSTANCE = it
            }
        }

        private fun buildDatabase(context: Context = BaseContentProvider.context()): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE History ADD COLUMN url TEXT NOT NULL DEFAULT ''")
                        db.execSQL("UPDATE History SET url = value")
                    }
                })
                .addMigrations(object : Migration(2, 3) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE User ADD COLUMN dark_theme INTEGER NOT NULL DEFAULT 0")
                    }
                })
                .addMigrations(object : Migration(3, 4) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS User_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                username TEXT,
                                nickname TEXT,
                                dark_theme TEXT
                            )""".trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO User_new (id, username, nickname, dark_theme)
                            SELECT id, username, nickname, CAST(dark_theme AS TEXT) FROM User
                            """.trimIndent()
                        )
                        db.execSQL("DROP TABLE User")
                        db.execSQL("ALTER TABLE User_new RENAME TO User")
                    }
                })
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

        @JvmStatic
        fun closeDB() {
            getDatabase().close()
        }

    }

    override fun close() {
        super.close()
        //数据库关闭后把instance置空
        INSTANCE = null
    }

}