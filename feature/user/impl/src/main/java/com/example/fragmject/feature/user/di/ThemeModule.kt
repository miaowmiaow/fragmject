package com.example.fragmject.feature.user.di

import com.example.fragmject.core.designsystem.ThemeState
import com.example.fragmject.feature.user.ThemeStateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 主题态契约绑定：把 [ThemeStateImpl] 绑定到 core:designsystem 的 [ThemeState]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeState(impl: ThemeStateImpl): ThemeState
}
