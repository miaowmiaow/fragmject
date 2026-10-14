package com.example.fragmject.core.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

/** 执行一条无结果集的 SQL（INSERT/UPDATE/DDL）。 */
internal fun SQLiteConnection.exec(sql: String) {
    prepare(sql).use { it.step() }
}

/** 执行查询并把每一行映射为 [T]。 */
internal fun <T> SQLiteConnection.query(sql: String, read: (SQLiteStatement) -> T): List<T> =
    prepare(sql).use { statement ->
        buildList {
            while (statement.step()) add(read(statement))
        }
    }

/** `sqlite_master` 中是否存在指定类型（table/index）与名称的对象。 */
internal fun SQLiteConnection.hasObject(type: String, name: String): Boolean =
    query("SELECT name FROM sqlite_master WHERE type = '$type' AND name = '$name'") { it.getText(0) }
        .isNotEmpty()

/** 指定表上的全部索引名。 */
internal fun SQLiteConnection.indexNames(table: String): Set<String> =
    query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = '$table' AND sql IS NOT NULL") {
        it.getText(0)
    }.toSet()
