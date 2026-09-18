package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.store.ThemeStore
import com.example.fragmject.core.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ThemeRepository] 领域端口的 data 层适配器。
 *
 * 直接委托 [ThemeStore]：写库后由 Room Flow 自动推送最新值，
 * 不再反向依赖 core:theme 的 DarkThemeState 全局状态。
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val themeStore: ThemeStore,
) : ThemeRepository {

    override fun observeDarkTheme(): Flow<Boolean> = themeStore.observeDarkTheme()

    override suspend fun setDarkTheme(dark: Boolean) {
        themeStore.setDarkTheme(dark)
    }
}
