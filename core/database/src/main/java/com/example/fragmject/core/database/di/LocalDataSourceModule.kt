package com.example.fragmject.core.database.di

import com.example.fragmject.core.data.contract.local.HistoryLocalDataSource
import com.example.fragmject.core.data.contract.local.HotKeyLocalDataSource
import com.example.fragmject.core.data.contract.local.NavigationLocalDataSource
import com.example.fragmject.core.data.contract.local.ProjectTreeLocalDataSource
import com.example.fragmject.core.data.contract.local.ScheduleLocalDataSource
import com.example.fragmject.core.data.contract.local.ThemeLocalDataSource
import com.example.fragmject.core.database.local.HistoryLocalDataSourceImpl
import com.example.fragmject.core.database.local.HotKeyLocalDataSourceImpl
import com.example.fragmject.core.database.local.NavigationLocalDataSourceImpl
import com.example.fragmject.core.database.local.ProjectTreeLocalDataSourceImpl
import com.example.fragmject.core.database.local.ScheduleLocalDataSourceImpl
import com.example.fragmject.core.database.local.ThemeLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 本地数据源的 Hilt 绑定：把各 Room/KV 适配器绑定到 data-contract 的本地端口。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindHistoryLocalDataSource(impl: HistoryLocalDataSourceImpl): HistoryLocalDataSource

    @Binds
    @Singleton
    abstract fun bindNavigationLocalDataSource(impl: NavigationLocalDataSourceImpl): NavigationLocalDataSource

    @Binds
    @Singleton
    abstract fun bindThemeLocalDataSource(impl: ThemeLocalDataSourceImpl): ThemeLocalDataSource

    @Binds
    @Singleton
    abstract fun bindScheduleLocalDataSource(impl: ScheduleLocalDataSourceImpl): ScheduleLocalDataSource

    @Binds
    @Singleton
    abstract fun bindHotKeyLocalDataSource(impl: HotKeyLocalDataSourceImpl): HotKeyLocalDataSource

    @Binds
    @Singleton
    abstract fun bindProjectTreeLocalDataSource(impl: ProjectTreeLocalDataSourceImpl): ProjectTreeLocalDataSource
}
