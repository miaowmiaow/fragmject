package com.example.fragmject.core.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.example.fragmject.core.database.dao.CoinRankDao
import com.example.fragmject.core.database.dao.HistoryDao
import com.example.fragmject.core.database.dao.HotKeyDao
import com.example.fragmject.core.database.dao.NavigationDao
import com.example.fragmject.core.database.dao.ProjectTreeDao
import com.example.fragmject.core.database.dao.TreeDao
import com.example.fragmject.core.database.dao.UserDao
import com.example.fragmject.core.database.model.CoinRankEntity
import com.example.fragmject.core.database.model.HistoryEntity
import com.example.fragmject.core.database.model.HotKeyEntity
import com.example.fragmject.core.database.model.NavigationEntity
import com.example.fragmject.core.database.model.ProjectTreeEntity
import com.example.fragmject.core.database.model.TreeEntity
import com.example.fragmject.core.database.model.UserEntity

@Database(
    entities = [HistoryEntity::class, UserEntity::class, TreeEntity::class, NavigationEntity::class, HotKeyEntity::class, ProjectTreeEntity::class, CoinRankEntity::class],
    version = 12,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun userDao(): UserDao
    abstract fun treeDao(): TreeDao
    abstract fun navigationDao(): NavigationDao
    abstract fun hotKeyDao(): HotKeyDao
    abstract fun projectTreeDao(): ProjectTreeDao
    abstract fun coinRankDao(): CoinRankDao

    companion object {

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_History_key_id` ON `History` (`key`, `id`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_History_key_value_id` ON `History` (`key`, `value`, `id`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_History_key_url_id` ON `History` (`key`, `url`, `id`)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(connection, "ALTER TABLE `User` DROP COLUMN `dark_theme`")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(connection, "ALTER TABLE `User` ADD COLUMN `icon` TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(
                    connection, """
                    CREATE TABLE IF NOT EXISTS `article` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `articleId` TEXT NOT NULL,
                        `cache_key` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `author` TEXT NOT NULL DEFAULT '',
                        `link` TEXT NOT NULL DEFAULT '',
                        `chapter_name` TEXT NOT NULL DEFAULT '',
                        `super_chapter_name` TEXT NOT NULL DEFAULT '',
                        `nice_date` TEXT NOT NULL DEFAULT '',
                        `envelope_pic` TEXT NOT NULL DEFAULT '',
                        `desc` TEXT NOT NULL DEFAULT '',
                        `collect` INTEGER NOT NULL DEFAULT 0,
                        `fresh` INTEGER NOT NULL DEFAULT 0,
                        `top` INTEGER NOT NULL DEFAULT 0,
                        `chapter_id` TEXT NOT NULL DEFAULT '',
                        `super_chapter_id` TEXT NOT NULL DEFAULT '',
                        `user_id` TEXT NOT NULL DEFAULT '',
                        `share_user` TEXT NOT NULL DEFAULT '',
                        `zan` TEXT NOT NULL DEFAULT '',
                        `tags_json` TEXT NOT NULL DEFAULT '',
                        `banners_json` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_article_cache_key_articleId` ON `article` (`cache_key`, `articleId`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_article_cache_key_sort_order` ON `article` (`cache_key`, `sort_order`)"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(
                    connection, """
                    CREATE TABLE IF NOT EXISTS `tree` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `treeId` TEXT NOT NULL,
                        `cache_key` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `course_id` TEXT NOT NULL DEFAULT '',
                        `order` TEXT NOT NULL DEFAULT '',
                        `parent_chapter_id` TEXT NOT NULL DEFAULT '',
                        `user_control_set_top` TEXT NOT NULL DEFAULT '',
                        `visible` TEXT NOT NULL DEFAULT '',
                        `children_json` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_tree_cache_key_treeId` ON `tree` (`cache_key`, `treeId`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_tree_cache_key_sort_order` ON `tree` (`cache_key`, `sort_order`)"
                )

                executeSql(
                    connection, """
                    CREATE TABLE IF NOT EXISTS `navigation` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `navId` TEXT NOT NULL,
                        `cache_key` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `cid` TEXT NOT NULL DEFAULT '',
                        `name` TEXT NOT NULL DEFAULT '',
                        `articles_json` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_navigation_cache_key_navId` ON `navigation` (`cache_key`, `navId`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_navigation_cache_key_sort_order` ON `navigation` (`cache_key`, `sort_order`)"
                )

                executeSql(
                    connection, """
                    CREATE TABLE IF NOT EXISTS `hot_key` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `hotKeyId` TEXT NOT NULL,
                        `cache_key` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `link` TEXT NOT NULL DEFAULT '',
                        `order` TEXT NOT NULL DEFAULT '',
                        `visible` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_hot_key_cache_key_hotKeyId` ON `hot_key` (`cache_key`, `hotKeyId`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_hot_key_cache_key_sort_order` ON `hot_key` (`cache_key`, `sort_order`)"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(
                    connection, """
                    CREATE TABLE IF NOT EXISTS `project_tree` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `cache_key` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `course_id` TEXT NOT NULL DEFAULT '',
                        `order` TEXT NOT NULL DEFAULT '',
                        `parent_chapter_id` TEXT NOT NULL DEFAULT '',
                        `user_control_set_top` TEXT NOT NULL DEFAULT '',
                        `visible` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_tree_cache_key_projectId` ON `project_tree` (`cache_key`, `projectId`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_project_tree_cache_key_sort_order` ON `project_tree` (`cache_key`, `sort_order`)"
                )

                executeSql(
                    connection, """
                    CREATE TABLE IF NOT EXISTS `coin_rank` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `cache_key` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `username` TEXT NOT NULL DEFAULT '',
                        `nickname` TEXT NOT NULL DEFAULT '',
                        `coin_count` TEXT NOT NULL DEFAULT '',
                        `level` TEXT NOT NULL DEFAULT '',
                        `rank` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_coin_rank_cache_key_userId` ON `coin_rank` (`cache_key`, `userId`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_coin_rank_cache_key_sort_order` ON `coin_rank` (`cache_key`, `sort_order`)"
                )
            }
        }

        /** 仅升级版本号：User → UserEntity 类名变更，表结构未变。 */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override suspend fun migrate(connection: SQLiteConnection) {
                // no-op: table structure unchanged
            }
        }

        /** 移除 article 缓存表：文章列表已改为纯网络分页，不再需要 Room 缓存。 */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(connection, "DROP TABLE IF EXISTS `article`")
            }
        }

        private fun executeSql(connection: SQLiteConnection, sql: String) {
            val statement = connection.prepare(sql)
            statement.use { statement ->
                statement.step()
            }
        }

        /** 构建数据库实例（由 Hilt DatabaseModule 调用并持有单例）。 */
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                .addMigrations(
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .build()
        }

    }
}