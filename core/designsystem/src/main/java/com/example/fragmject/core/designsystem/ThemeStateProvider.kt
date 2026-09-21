package com.example.fragmject.core.designsystem

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用级主题态契约，供壳层观察深色模式。
 *
 * 定义在 core:designsystem，使 app 层只依赖本接口，不再直接消费
 * [com.example.fragmject.core.domain.repository.ThemeRepository]。
 */
interface ThemeStateProvider {
    val darkTheme: StateFlow<Boolean>
}
