package com.example.fragmject.feature.user.di

import com.example.fragmject.core.android.platform.AppScope
import com.example.fragmject.core.designsystem.ThemeStateProvider
import com.example.fragmject.core.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主题态契约实现：把 [ThemeRepository.observeDarkTheme] 暴露为应用级深色模式状态。
 *
 * 放在 feature:user:impl（主题设置 UI 所在模块），使 app 只依赖 core:designsystem
 * 的 [ThemeStateProvider]，不再 import ThemeRepository。
 */
@Singleton
class ThemeStateProviderImpl @Inject constructor(
    themeRepository: ThemeRepository,
) : ThemeStateProvider {
    override val darkTheme: StateFlow<Boolean> =
        themeRepository.observeDarkTheme()
            .stateIn(AppScope, SharingStarted.Eagerly, false)
}
