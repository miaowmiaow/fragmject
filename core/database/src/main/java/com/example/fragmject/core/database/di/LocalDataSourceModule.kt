package com.example.fragmject.core.database.di

import com.example.fragmject.core.data.contract.local.HomeNavLocalDataSource
import com.example.fragmject.core.data.contract.local.HotKeyLocalDataSource
import com.example.fragmject.core.data.contract.local.ProjectTreeLocalDataSource
import com.example.fragmject.core.data.contract.local.SystemTreeLocalDataSource
import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import com.example.fragmject.core.domain.repository.ScheduleRepository
import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.database.local.HistoryLocalDataSourceImpl
import com.example.fragmject.core.database.local.HomeNavLocalDataSourceImpl
import com.example.fragmject.core.database.local.HotKeyLocalDataSourceImpl
import com.example.fragmject.core.database.local.OtpAccountRepositoryImpl
import com.example.fragmject.core.database.local.ProjectTreeLocalDataSourceImpl
import com.example.fragmject.core.database.local.ScheduleLocalDataSourceImpl
import com.example.fragmject.core.database.local.SystemTreeLocalDataSourceImpl
import com.example.fragmject.core.database.local.ThemeLocalDataSourceImpl
import com.example.fragmject.core.database.local.UserLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 本地数据源的 Hilt 绑定。
 *
 * 其余 5 个（HomeNav/HotKey/ProjectTree/SystemTree/User）绑定到 data-contract 本地端口，
 * 再由 data-repository 实现 domain 端口。
 *
 * 白名单例外 4 个（History/Schedule/Theme/OtpAccount）是零逻辑链，database 直接实现
 * domain 端口、不写中间透传层，故这里同时出现两类绑定。白名单由架构测试规则 18
 * （`core database only implements whitelisted domain ports`）锁定，新增需评审。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryLocalDataSourceImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindHomeNavLocalDataSource(impl: HomeNavLocalDataSourceImpl): HomeNavLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSystemTreeLocalDataSource(impl: SystemTreeLocalDataSourceImpl): SystemTreeLocalDataSource

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeLocalDataSourceImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleLocalDataSourceImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindHotKeyLocalDataSource(impl: HotKeyLocalDataSourceImpl): HotKeyLocalDataSource

    @Binds
    @Singleton
    abstract fun bindProjectTreeLocalDataSource(impl: ProjectTreeLocalDataSourceImpl): ProjectTreeLocalDataSource

    @Binds
    @Singleton
    abstract fun bindUserLocalDataSource(impl: UserLocalDataSourceImpl): UserLocalDataSource

    @Binds
    @Singleton
    abstract fun bindOtpAccountRepository(
        impl: OtpAccountRepositoryImpl,
    ): OtpAccountRepository
}
