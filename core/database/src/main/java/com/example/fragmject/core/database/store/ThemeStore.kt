package com.example.fragmject.core.database.store

import com.example.fragmject.core.database.KVDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 深色模式持久化存储 facade。
 *
 * 以 Room 的 Flow 查询作为深色模式的唯一数据源：写库后 Room 会自动向
 * [observeDarkTheme] 的订阅者推送最新值，不再需要 data 层维护独立的内存状态。
 */
@Singleton
class ThemeStore @Inject constructor(
    private val kvDatabase: KVDatabase,
) {

    private companion object {
        const val KEY_DARK_THEME = "dark_theme"
    }

    fun observeDarkTheme(): Flow<Boolean> =
        kvDatabase.observeValue(KEY_DARK_THEME).map { it.toBoolean() }

    suspend fun setDarkTheme(dark: Boolean): Boolean {
        return kvDatabase.setValue(KEY_DARK_THEME, dark.toString())
    }
}
