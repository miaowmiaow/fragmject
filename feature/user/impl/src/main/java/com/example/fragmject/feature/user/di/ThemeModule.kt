package com.example.fragmject.feature.user.di

import com.example.fragmject.core.designsystem.ThemeStateProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 主题态契约绑定：把 [ThemeStateProviderImpl] 绑定到 core:designsystem 的 [ThemeStateProvider]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeStateProvider(impl: ThemeStateProviderImpl): ThemeStateProvider
}
