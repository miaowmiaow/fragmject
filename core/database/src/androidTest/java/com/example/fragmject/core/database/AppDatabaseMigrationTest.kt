package com.example.fragmject.core.database

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fragmject.core.database.model.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AppDatabase] 迁移测试：基于 `core/database/schemas` 导出的 schema，在真机/模拟器的
 * 系统 SQLite（与生产默认驱动一致）上逐步验证迁移结果与数据保留。
 *
 * 说明：
 * - 使用 [AppDatabase.MIGRATIONS]（生产注册的同一份列表），不在测试里重复拼装。
 * - `MigrationTestHelper` 不校验“多余的表”，因此对删表/改名迁移额外断言 `sqlite_master`。
 * - 历史 schema 只有 4、6、8 及以后：7 从未导出；5.json 曾被覆盖（与 6.json 哈希相同，
 *   `User` 已无 `dark_theme`，与 5→6 迁移矛盾），因此不能作为 v5 基线。
 *   旧版本只能经 v4/v6 起点的整链用例验证（4→5→6 的真实路径由 v4 用例覆盖）。
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = context.getDatabasePath(TEST_DB),
        driver = AndroidSQLiteDriver(),
        databaseClass = AppDatabase::class,
    )

    private val latestVersion = AppDatabase.MIGRATIONS.maxOf { it.endVersion }

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun registeredMigrationsFormAContiguousChain() {
        val sorted = AppDatabase.MIGRATIONS.sortedBy { it.startVersion }
        sorted.zipWithNext { current, next ->
            assertEquals(
                "迁移 ${current.startVersion}→${current.endVersion} 与下一条 ${next.startVersion}→${next.endVersion} 不连续",
                current.endVersion,
                next.startVersion,
            )
        }
        sorted.forEach {
            assertEquals("迁移必须逐版本升级: ${it.startVersion}→${it.endVersion}", it.startVersion + 1, it.endVersion)
        }
    }

    @Test
    fun eachStepFrom8ToLatestMatchesExportedSchema() = runBlocking<Unit> {
        for (from in 8 until latestVersion) {
            context.deleteDatabase(TEST_DB)
            helper.createDatabase(from).close()
            try {
                helper.runMigrationsAndValidate(from + 1, AppDatabase.MIGRATIONS.toList()).close()
            } catch (e: Throwable) {
                throw AssertionError("迁移 $from→${from + 1} 后结构与 ${from + 1}.json 不一致", e)
            }
        }
    }

    @Test
    fun migration11To12DropsArticleTable() = runBlocking<Unit> {
        helper.createDatabase(11).use { db ->
            db.exec(
                "INSERT INTO article (articleId, cache_key, sort_order, title, author, link, chapter_name, " +
                    "super_chapter_name, nice_date, envelope_pic, `desc`, collect, fresh, top, chapter_id, " +
                    "super_chapter_id, user_id, share_user, zan, tags_json, banners_json, timestamp) " +
                    "VALUES ('1', 'home', 0, 'a', '', '', '', '', '', '', '', 0, 0, 0, '', '', '', '', '', '', '', 0)"
            )
            assertTrue(db.hasObject("table", "article"))
        }

        helper.runMigrationsAndValidate(12, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertFalse("article 表应被删除", db.hasObject("table", "article"))
            assertTrue(db.hasObject("table", "tree"))
        }
    }

    @Test
    fun migration12To13RenamesNavigationKeepingRowsAndIndices() = runBlocking<Unit> {
        helper.createDatabase(12).use { db ->
            db.exec(
                "INSERT INTO navigation (navId, cache_key, sort_order, cid, name, articles_json, timestamp) " +
                    "VALUES ('n1', 'home', 3, 'c1', '导航', '[]', 99)"
            )
        }

        helper.runMigrationsAndValidate(13, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertFalse("旧表 navigation 应被重命名", db.hasObject("table", "navigation"))
            val rows = db.query("SELECT navId, cache_key, sort_order, name, timestamp FROM home_nav") {
                listOf(it.getText(0), it.getText(1), it.getLong(2), it.getText(3), it.getLong(4))
            }
            assertEquals(listOf(listOf("n1", "home", 3L, "导航", 99L)), rows)
            assertEquals(
                setOf("index_home_nav_cache_key_navId", "index_home_nav_cache_key_sort_order"),
                db.indexNames("home_nav"),
            )
            assertFalse(db.hasObject("index", "index_navigation_cache_key_navId"))
        }
    }

    @Test
    fun migration13To14DropsCoinRankTable() = runBlocking<Unit> {
        helper.createDatabase(13).use { db ->
            assertTrue("13 版本应仍有 coin_rank 表", db.hasObject("table", "coin_rank"))
        }

        helper.runMigrationsAndValidate(14, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertFalse("coin_rank 表应被删除", db.hasObject("table", "coin_rank"))
        }
    }

    @Test
    fun userAndHistoryDataSurviveMigrationFrom8ToLatest() = runBlocking<Unit> {
        helper.createDatabase(8).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, icon) VALUES (7, 'tom', 'Tom', 'icon.png')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('search', 'kotlin', '')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('browse', 'Room', 'https://a.b')")
        }

        helper.runMigrationsAndValidate(latestVersion, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertEquals(
                listOf(listOf<Any?>(7L, "tom", "Tom", "icon.png")),
                db.query("SELECT id, username, nickname, icon FROM User") {
                    listOf(it.getLong(0), it.getText(1), it.getText(2), it.getText(3))
                },
            )
            assertEquals(
                listOf("search|kotlin|", "browse|Room|https://a.b"),
                db.query("SELECT `key`, value, url FROM History ORDER BY id") {
                    "${it.getText(0)}|${it.getText(1)}|${it.getText(2)}"
                },
            )
            assertFalse(db.hasObject("table", "article"))
            assertFalse(db.hasObject("table", "navigation"))
            assertFalse(db.hasObject("table", "coin_rank"))
        }
    }

    @Test
    fun legacyVersion4MigratesToLatestKeepingUserAndHistory() = runBlocking<Unit> {
        helper.createDatabase(4).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, dark_theme) VALUES (1, 'old', 'Old', '1')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('search', 'legacy', '')")
        }

        helper.runMigrationsAndValidate(latestVersion, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertLegacyRowsKept(db, id = 1L, username = "old", historyValue = "legacy")
        }
    }

    @Test
    fun legacyVersion6MigratesToLatestAddingIconColumn() = runBlocking<Unit> {
        helper.createDatabase(6).use { db ->
            db.exec("INSERT INTO User (id, username, nickname) VALUES (2, 'mid', 'Mid')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('search', 'v6', '')")
        }

        helper.runMigrationsAndValidate(latestVersion, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertLegacyRowsKept(db, id = 2L, username = "mid", historyValue = "v6")
        }
    }

    @Test
    fun migratedDatabaseOpensWithRealRoomAndDaoReadsRow() = runBlocking<Unit> {
        helper.createDatabase(8).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, icon) VALUES (9, 'jerry', 'Jerry', NULL)")
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
        helper.closeWhenFinished(database)

        val user = database.userDao().get().first()
        assertEquals(UserEntity(id = 9, username = "jerry", nickname = "Jerry"), user)
        database.close()
    }

    @Test
    fun freshInstallCreatesLatestSchemaAndUserReplaceIsAtomicReplacement() = runBlocking<Unit> {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
        helper.closeWhenFinished(database)
        val dao = database.userDao()

        assertNull(dao.get().first())
        dao.insert(UserEntity(id = 1, username = "first"))
        dao.replace(UserEntity(id = 2, username = "second"))

        assertEquals("second", dao.get().first()?.username)
        database.close()
    }

    private fun assertLegacyRowsKept(
        db: SQLiteConnection,
        id: Long,
        username: String,
        historyValue: String,
    ) {
        assertEquals(
            listOf(listOf<Any?>(id, username)),
            db.query("SELECT id, username FROM User") { listOf(it.getLong(0), it.getText(1)) },
        )
        assertTrue("迁移后应已有 icon 列", db.query("SELECT icon FROM User") { it.isNull(0) }.single())
        assertEquals(
            listOf(historyValue),
            db.query("SELECT value FROM History") { it.getText(0) },
        )
    }

    private companion object {
        const val TEST_DB = "app-migration-test.db"
    }
}
