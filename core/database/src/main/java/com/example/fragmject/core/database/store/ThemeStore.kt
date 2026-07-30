package com.example.fragmject.core.database.store

import com.example.fragmject.core.common.DarkThemeState
import com.example.fragmject.core.database.KVDatabase

/**
 * 深色模式持久化存储 facade。
 *
 * 封装 KVDatabase 读写 + DarkThemeState 通知，供 ViewModel 直接使用。
 */
object ThemeStore {

    private const val KEY_DARK_THEME = "dark_theme"

    suspend fun setDarkTheme(dark: Boolean) {
        KVDatabase.set(KEY_DARK_THEME, dark.toString())
        DarkThemeState.isDark.value = dark
    }

    suspend fun restoreDarkTheme() {
        val value = KVDatabase.get(KEY_DARK_THEME)
        DarkThemeState.isDark.value = value.toBoolean()
    }
}
