package com.example.fragmject.core.database

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fragmject.core.database.crypto.UserFieldCipher
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

    /** 15→16 起 user 表 PII 落地为密文，断言前先解密（Keystore 在真机可用）。 */
    private val cipher = UserFieldCipher()

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
    fun eachStepFrom8ToLatestMatchesExportedSchema() = runBlocking {
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
    fun migration11To12DropsArticleTable() = runBlocking {
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
    fun migration12To13RenamesNavigationKeepingRowsAndIndices() = runBlocking {
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
    fun migration13To14DropsCoinRankTable() = runBlocking {
        helper.createDatabase(13).use { db ->
            assertTrue("13 版本应仍有 coin_rank 表", db.hasObject("table", "coin_rank"))
        }

        helper.runMigrationsAndValidate(14, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertFalse("coin_rank 表应被删除", db.hasObject("table", "coin_rank"))
        }
    }

    @Test
    fun migration14To15CreatesOtpAccountTableKeepingExistingData() = runBlocking {
        helper.createDatabase(14).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, icon) VALUES (1, 'keep', 'Keep', NULL)")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('search', 'keepme', '')")
        }

        helper.runMigrationsAndValidate(latestVersion, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertTrue("otp_account 表应被创建", db.hasObject("table", "otp_account"))
            assertTrue(
                "otp_account 应有 sort_order 索引",
                db.indexNames("otp_account").contains("index_otp_account_sort_order"),
            )
            assertEquals(
                listOf(
                    "id", "issuer", "account_name", "secret_cipher", "algorithm",
                    "digits", "period", "type", "counter", "sort_order", "created_at",
                ),
                db.query("PRAGMA table_info(otp_account)") { it.getText(1) },
            )
            // 已有数据不得丢失（15→16 起 username 是密文，解密后应还原）
            assertEquals(
                listOf("keep"),
                db.query("SELECT username FROM User") { it.getText(0) }.map { cipher.decryptOrPlainText(it) },
            )
            assertEquals(listOf("keepme"), db.query("SELECT value FROM History") { it.getText(0) })
        }
    }

    @Test
    fun migration15To16EncryptsUserPii() = runBlocking {
        helper.createDatabase(15).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, icon) VALUES (1, 'plain', 'Plain', 'icon.png')")
            // icon 为 NULL 的历史行：Room3 的 SQLiteStatement.getText 对 NULL 列直接抛 NPE，
            // 这个用例曾经让整段迁移崩掉并触发兜底清表（真机 / 模拟器实测）
            db.exec("INSERT INTO User (id, username, nickname, icon) VALUES (2, NULL, NULL, NULL)")
        }

        helper.runMigrationsAndValidate(16, AppDatabase.MIGRATIONS.toList()).use { db ->
            val rows = db.query("SELECT id, username, nickname, icon FROM User ORDER BY id") { stmt ->
                listOf(
                    stmt.getLong(0),
                    if (stmt.isNull(1)) null else stmt.getText(1),
                    if (stmt.isNull(2)) null else stmt.getText(2),
                    if (stmt.isNull(3)) null else stmt.getText(3),
                )
            }
            assertEquals("两行都应保留", 2, rows.size)

            val filled = rows[0]
            listOf(filled[1], filled[2], filled[3]).forEach { stored ->
                val text = stored as String?
                assertTrue("PII 字段应存密文（v1: 前缀），实际：$text", text.orEmpty().startsWith("v1:"))
            }
            assertEquals("plain", cipher.decryptOrPlainText(filled[1] as String?))
            assertEquals("Plain", cipher.decryptOrPlainText(filled[2] as String?))
            assertEquals("icon.png", cipher.decryptOrPlainText(filled[3] as String?))

            // 全 NULL 行迁移后仍应是 NULL：不能被解成空串，也不能因为异常被删除
            assertEquals(listOf<Any?>(2L, null, null, null), rows[1])
        }
    }

    @Test
    fun userAndHistoryDataSurviveMigrationFrom8ToLatest() = runBlocking {
        helper.createDatabase(8).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, icon) VALUES (7, 'tom', 'Tom', 'icon.png')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('search', 'kotlin', '')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('browse', 'Room', 'https://a.b')")
        }

        helper.runMigrationsAndValidate(latestVersion, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertEquals(
                listOf(listOf<Any?>(7L, "tom", "Tom", "icon.png")),
                db.query("SELECT id, username, nickname, icon FROM User") {
                    listOf(
                        it.getLong(0),
                        cipher.decryptOrPlainText(it.getText(1)),
                        cipher.decryptOrPlainText(it.getText(2)),
                        cipher.decryptOrPlainText(it.getText(3)),
                    )
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
    fun legacyVersion4MigratesToLatestKeepingUserAndHistory() = runBlocking {
        helper.createDatabase(4).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, dark_theme) VALUES (1, 'old', 'Old', '1')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('search', 'legacy', '')")
        }

        helper.runMigrationsAndValidate(latestVersion, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertLegacyRowsKept(db, id = 1L, username = "old", historyValue = "legacy")
        }
    }

    @Test
    fun legacyVersion6MigratesToLatestAddingIconColumn() = runBlocking {
        helper.createDatabase(6).use { db ->
            db.exec("INSERT INTO User (id, username, nickname) VALUES (2, 'mid', 'Mid')")
            db.exec("INSERT INTO History (`key`, value, url) VALUES ('search', 'v6', '')")
        }

        helper.runMigrationsAndValidate(latestVersion, AppDatabase.MIGRATIONS.toList()).use { db ->
            assertLegacyRowsKept(db, id = 2L, username = "mid", historyValue = "v6")
        }
    }

    @Test
    fun migratedDatabaseOpensWithRealRoomAndDaoReadsRow() = runBlocking {
        helper.createDatabase(8).use { db ->
            db.exec("INSERT INTO User (id, username, nickname, icon) VALUES (9, 'jerry', 'Jerry', NULL)")
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
        helper.closeWhenFinished(database)

        val user = database.userDao().get().first()
        // 库里存的是密文，直接读 DAO 得到的是密文；解密后才与迁移前的明文一致
        assertEquals(
            UserEntity(id = 9, username = "jerry", nickname = "Jerry"),
            user?.copy(
                username = cipher.decryptOrPlainText(user.username),
                nickname = cipher.decryptOrPlainText(user.nickname),
                icon = cipher.decryptOrPlainText(user.icon),
            ),
        )
        database.close()
    }

    @Test
    fun freshInstallCreatesLatestSchemaAndUserReplaceIsAtomicReplacement() = runBlocking {
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
            db.query("SELECT id, username FROM User") {
                listOf(it.getLong(0), cipher.decryptOrPlainText(it.getText(1)))
            },
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
