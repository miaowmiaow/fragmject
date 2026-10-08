package com.example.fragmject.core.data.repository.di

import com.example.fragmject.core.data.repository.AuthStateImpl
import com.example.fragmject.core.data.repository.AlbumRepositoryImpl
import com.example.fragmject.core.data.repository.media.MediaEditorImpl
import com.example.fragmject.core.data.repository.system.SystemStorageImpl
import com.example.fragmject.core.data.repository.CoinRankRepositoryImpl
import com.example.fragmject.core.data.repository.CollectStateImpl
import com.example.fragmject.core.data.repository.DownloadRepositoryImpl
import com.example.fragmject.core.data.repository.HistoryRepositoryImpl
import com.example.fragmject.core.data.repository.HomeRepositoryImpl
import com.example.fragmject.core.data.repository.MediaRepositoryImpl
import com.example.fragmject.core.data.repository.MyCollectRepositoryImpl
import com.example.fragmject.core.data.repository.MyRepositoryImpl
import com.example.fragmject.core.data.repository.HomeNavRepositoryImpl
import com.example.fragmject.core.data.repository.ProjectRepositoryImpl
import com.example.fragmject.core.data.repository.ScheduleRepositoryImpl
import com.example.fragmject.core.data.repository.SearchRepositoryImpl
import com.example.fragmject.core.data.repository.SystemRepositoryImpl
import com.example.fragmject.core.data.repository.ThemeRepositoryImpl
import com.example.fragmject.core.data.repository.ThemeStateImpl
import com.example.fragmject.core.data.repository.UserCenterRepositoryImpl
import com.example.fragmject.core.data.repository.UserRepositoryImpl
import com.example.fragmject.core.domain.AuthState
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.ThemeState
import com.example.fragmject.core.domain.media.MediaEditor
import com.example.fragmject.core.domain.system.SystemStorage
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.CoinRankRepository
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.repository.HomeNavRepository
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.repository.ScheduleRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 领域端口 Adapter 绑定。
 *
 * 使用 [Binds] 抽象方法在接口与实现之间建立桥接，替代原先的 [dagger.Provides]
 * 工厂方法，减少 Dagger 生成的 Factory 类数量、加快编译并缩小 dex。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindMyRepository(impl: MyRepositoryImpl): MyRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindThemeState(impl: ThemeStateImpl): ThemeState

    @Binds
    @Singleton
    abstract fun bindCollectState(impl: CollectStateImpl): CollectState

    @Binds
    @Singleton
    abstract fun bindAuthState(impl: AuthStateImpl): AuthState

    @Binds
    @Singleton
    abstract fun bindHomeNavRepository(impl: HomeNavRepositoryImpl): HomeNavRepository

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindCoinRankRepository(impl: CoinRankRepositoryImpl): CoinRankRepository

    @Binds
    @Singleton
    abstract fun bindSystemRepository(impl: SystemRepositoryImpl): SystemRepository

    @Binds
    @Singleton
    abstract fun bindMyCollectRepository(impl: MyCollectRepositoryImpl): MyCollectRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindUserCenterRepository(impl: UserCenterRepositoryImpl): UserCenterRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: AlbumRepositoryImpl): AlbumRepository

    // internal：MediaEditorImpl 为 internal（架构测试规则 16 约束 media 包对外表面），
    // Kotlin 不允许 public 函数暴露 internal 类型；internal 方法在字节码中仍是 public，Dagger 可正常处理
    @Binds
    @Singleton
    internal abstract fun bindMediaEditor(impl: MediaEditorImpl): MediaEditor

    @Binds
    @Singleton
    abstract fun bindSystemStorage(impl: SystemStorageImpl): SystemStorage
}