package com.example.fragmject.core.data.contract.local

import kotlinx.coroutines.flow.Flow

/**
 * 深色模式本地数据源。
 *
 * 由 core:database 使用 KV 存储实现；core:data 只依赖本契约。
 */
interface ThemeLocalDataSource {
    fun observeDarkTheme(): Flow<Boolean>
    suspend fun setDarkTheme(dark: Boolean)
}
