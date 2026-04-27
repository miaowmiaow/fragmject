package com.example.fragment.project.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
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