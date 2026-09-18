package com.example.fragmject.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 深色模式领域端口。
 *
 * 只暴露 [Flow]<[Boolean]> 供 UI 观察，写入通过 [setDarkTheme] 持久化；
 * 初始值由 Room 的 Flow 查询在订阅时自动推送，无需单独的 restore 方法。
 */
interface ThemeRepository {
    fun observeDarkTheme(): Flow<Boolean>
    suspend fun setDarkTheme(dark: Boolean)
}
