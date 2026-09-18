package com.example.fragmject.core.data.di

import com.example.fragmject.core.data.repository.AlbumRepositoryImpl
import com.example.fragmject.core.data.repository.DownloadRepositoryImpl
import com.example.fragmject.core.data.repository.HistoryRepositoryImpl
import com.example.fragmject.core.data.repository.MediaRepositoryImpl
import com.example.fragmject.core.data.repository.MyRepositoryImpl
import com.example.fragmject.core.data.repository.OfflineFirstArticleRepository
import com.example.fragmject.core.data.repository.OfflineFirstCoinRankRepository
import com.example.fragmject.core.data.repository.OfflineFirstMyCollectRepository
import com.example.fragmject.core.data.repository.OfflineFirstNavigationRepository
import com.example.fragmject.core.data.repository.OfflineFirstProjectRepository
import com.example.fragmject.core.data.repository.OfflineFirstSystemRepository
import com.example.fragmject.core.data.repository.ScheduleRepositoryImpl
import com.example.fragmject.core.data.repository.SearchRepositoryImpl
import com.example.fragmject.core.data.repository.ThemeRepositoryImpl
import com.example.fragmject.core.data.repository.UserCenterRepositoryImpl
import com.example.fragmject.core.data.repository.UserRepositoryImpl
import com.example.fragmject.core.data.repository.VideoDownloadRepositoryImpl
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.CoinRankRepository
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.repository.ScheduleRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.repository.VideoDownloadRepository
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
    abstract fun bindNavigationRepository(impl: OfflineFirstNavigationRepository): NavigationRepository

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: OfflineFirstArticleRepository): HomeRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: OfflineFirstProjectRepository): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindCoinRankRepository(impl: OfflineFirstCoinRankRepository): CoinRankRepository

    @Binds
    @Singleton
    abstract fun bindSystemRepository(impl: OfflineFirstSystemRepository): SystemRepository

    @Binds
    @Singleton
    abstract fun bindMyCollectRepository(impl: OfflineFirstMyCollectRepository): MyCollectRepository

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

    @Binds
    @Singleton
    abstract fun bindVideoDownloadRepository(impl: VideoDownloadRepositoryImpl): VideoDownloadRepository
}
