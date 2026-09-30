package com.example.fragmject.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级主题态契约，供壳层观察深色模式。
 *
 * 与 [com.example.fragmject.core.domain.repository.ThemeRepository] 同属 core:domain，
 * 由 core:data-repository 提供实现并绑定，app 层只依赖本接口，不再依赖 feature 提供实现。
 */
interface ThemeState {
    val darkTheme: StateFlow<Boolean>
}
