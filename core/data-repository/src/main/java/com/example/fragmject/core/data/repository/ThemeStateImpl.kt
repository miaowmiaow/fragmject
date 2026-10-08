package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.ThemeState
import com.example.fragmject.core.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主题态实现：把 [ThemeRepository.observeDarkTheme] 暴露为应用级深色模式状态。
 *
 * 下沉到 core:data-repository（领域端口装配层），与 AuthState 契约/实现归位方式一致，
 * 使 core 不再依赖 feature 提供实现，裁剪 feature:user:impl 不会导致全局主题态缺 binding。
 */
@Singleton
class ThemeStateImpl @Inject constructor(
    themeRepository: ThemeRepository,
    appScope: AppCoroutineScope,
) : ThemeState {
    override val darkTheme: StateFlow<Boolean> =
        themeRepository.observeDarkTheme()
            .stateIn(appScope, SharingStarted.Eagerly, false)
}
