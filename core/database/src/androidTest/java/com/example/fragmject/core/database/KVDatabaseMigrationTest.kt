package com.example.fragmject.core.database

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** [KVDatabase] 迁移测试：v1→v2 的去重与唯一索引。 */
@RunWith(AndroidJUnit4::class)
class KVDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = context.getDatabasePath(TEST_DB),
        driver = AndroidSQLiteDriver(),
        databaseClass = KVDatabase::class,
    )

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migration1To2KeepsNewestRowPerKeyAndAddsUniqueIndex() = runBlocking<Unit> {
        helper.createDatabase(1).use { db ->
            db.exec("INSERT INTO kv_table (first, second) VALUES ('a', '1')")
            db.exec("INSERT INTO kv_table (first, second) VALUES ('b', 'x')")
            db.exec("INSERT INTO kv_table (first, second) VALUES ('a', '2')")
            db.exec("INSERT INTO kv_table (first, second) VALUES ('a', '3')")
        }

        helper.runMigrationsAndValidate(2, KVDatabase.MIGRATIONS.toList()).use { db ->
            assertEquals(
                listOf("a" to "3", "b" to "x"),
                db.query("SELECT first, second FROM kv_table ORDER BY first") {
                    it.getText(0) to it.getText(1)
                },
            )
            assertTrue(db.indexNames("kv_table").contains("index_kv_table_first"))
            assertThrows(Exception::class.java) {
                db.exec("INSERT INTO kv_table (first, second) VALUES ('a', 'dup')")
            }
        }
    }

    @Test
    fun migratedDatabaseOverwritesSameKeyThroughRealRoom() = runBlocking<Unit> {
        helper.createDatabase(1).use { db ->
            db.exec("INSERT INTO kv_table (first, second) VALUES ('theme', 'light')")
            db.exec("INSERT INTO kv_table (first, second) VALUES ('theme', 'dark')")
        }

        val database = Room.databaseBuilder(context, KVDatabase::class.java, TEST_DB)
            .addMigrations(*KVDatabase.MIGRATIONS)
            .build()
        helper.closeWhenFinished(database)

        assertEquals("dark", database.getValue("theme"))
        assertTrue(database.setValue("theme", "auto"))
        assertEquals("auto", database.getValue("theme"))
        database.close()
    }

    private companion object {
        const val TEST_DB = "kv-migration-test.db"
    }
}
