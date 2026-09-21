package com.example.fragmject.core.database.local

import com.example.fragmject.core.database.KVDatabase
import com.example.fragmject.core.data.contract.local.ThemeLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ThemeLocalDataSource] 的 KV 适配器实现。
 */
@Singleton
class ThemeLocalDataSourceImpl @Inject constructor(
    private val kvDatabase: KVDatabase,
) : ThemeLocalDataSource {

    private companion object {
        const val KEY_DARK_THEME = "dark_theme"
    }

    override fun observeDarkTheme(): Flow<Boolean> =
        kvDatabase.observeValue(KEY_DARK_THEME).map { it.toBoolean() }

    override suspend fun setDarkTheme(dark: Boolean) {
        kvDatabase.setValue(KEY_DARK_THEME, dark.toString())
    }
}
