package com.example.fragmject.core.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.example.fragmject.core.database.dao.HistoryDao
import com.example.fragmject.core.database.dao.HomeNavDao
import com.example.fragmject.core.database.dao.HotKeyDao
import com.example.fragmject.core.database.dao.OtpAccountDao
import com.example.fragmject.core.database.dao.ProjectTreeDao
import com.example.fragmject.core.database.dao.TreeDao
import com.example.fragmject.core.database.dao.UserDao
import com.example.fragmject.core.database.model.HistoryEntity
import com.example.fragmject.core.database.model.HotKeyEntity
import com.example.fragmject.core.database.model.HomeNavEntity
import com.example.fragmject.core.database.model.OtpAccountEntity
import com.example.fragmject.core.database.model.ProjectTreeEntity
import com.example.fragmject.core.database.model.TreeEntity
import com.example.fragmject.core.database.model.UserEntity

@Database(
    entities = [
        HistoryEntity::class,
        UserEntity::class,
        TreeEntity::class,
        HomeNavEntity::class,
        HotKeyEntity::class,
        ProjectTreeEntity::class,
        OtpAccountEntity::class,
    ],
    version = 15,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun userDao(): UserDao
    abstract fun treeDao(): TreeDao
    abstract fun homeNavDao(): HomeNavDao
    abstract fun hotKeyDao(): HotKeyDao
    abstract fun projectTreeDao(): ProjectTreeDao
    abstract fun otpAccountDao(): OtpAccountDao

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
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_tree_cache_key_projectId` " +
                        "ON `project_tree` (`cache_key`, `projectId`)"
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

        /** 重命名 navigation 表为 home_nav，并同步重建索引名以匹配新 schema。 */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(connection, "ALTER TABLE `navigation` RENAME TO `home_nav`")
                executeSql(connection, "DROP INDEX IF EXISTS `index_navigation_cache_key_navId`")
                executeSql(
                    connection,
                    "DROP INDEX IF EXISTS `index_navigation_cache_key_sort_order`"
                )
                executeSql(
                    connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_home_nav_cache_key_navId` ON `home_nav` (`cache_key`, `navId`)"
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_home_nav_cache_key_sort_order` ON `home_nav` (`cache_key`, `sort_order`)"
                )
            }
        }

        /** 移除 coin_rank 缓存表：积分排行榜已改为纯网络分页，不再需要 Room 缓存。 */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(connection, "DROP TABLE IF EXISTS `coin_rank`")
            }
        }

        /**
         * 新增 otp_account 表：Authenticator（TOTP）账户。
         *
         * `secret_cipher` 只存密文（IV||cipherText，见 `AesGcmCipher`），明文密钥不落库。
         */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override suspend fun migrate(connection: SQLiteConnection) {
                executeSql(
                    connection, """
                    CREATE TABLE IF NOT EXISTS `otp_account` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `issuer` TEXT NOT NULL,
                        `account_name` TEXT NOT NULL,
                        `secret_cipher` BLOB NOT NULL,
                        `algorithm` INTEGER NOT NULL,
                        `digits` INTEGER NOT NULL,
                        `period` INTEGER NOT NULL,
                        `type` INTEGER NOT NULL,
                        `counter` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                executeSql(
                    connection,
                    "CREATE INDEX IF NOT EXISTS `index_otp_account_sort_order` ON `otp_account` (`sort_order`)"
                )
            }
        }

        private fun executeSql(connection: SQLiteConnection, sql: String) {
            val statement = connection.prepare(sql)
            statement.use { statement ->
                statement.step()
            }
        }

        /**
         * 生产环境注册的全部迁移。升级 [Database.version] 时必须在此追加对应迁移，
         * 并导出新 schema；`AppDatabaseMigrationTest` 以此列表校验迁移链完整且结果与 schema 一致。
         */
        internal val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
        )

        /** 构建数据库实例（由 Hilt DatabaseModule 调用并持有单例）。 */
        fun build(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            // Room 只提供 vararg 重载，逐个注册以免 `*MIGRATIONS` 展开
            MIGRATIONS.forEach { builder.addMigrations(it) }
            return builder.build()
        }

    }
}