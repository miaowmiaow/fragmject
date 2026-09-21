package com.example.fragmject.core.data.impl.repository

import com.example.fragmject.core.data.contract.local.ThemeLocalDataSource
import com.example.fragmject.core.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ThemeRepository] 领域端口的 data 层适配器。
 *
 * 依赖 data-contract 的 [ThemeLocalDataSource]，不感知 KV 存储实现。
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val local: ThemeLocalDataSource,
) : ThemeRepository {

    override fun observeDarkTheme(): Flow<Boolean> = local.observeDarkTheme()

    override suspend fun setDarkTheme(dark: Boolean) {
        local.setDarkTheme(dark)
    }
}
