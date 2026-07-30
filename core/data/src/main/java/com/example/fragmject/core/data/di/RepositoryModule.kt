package com.example.fragmject.core.data.di

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.data.repository.ArticleRepository
import com.example.fragmject.core.data.repository.ArticleRepositoryImpl
import com.example.fragmject.core.data.repository.CommonRepository
import com.example.fragmject.core.data.repository.CommonRepositoryImpl
import com.example.fragmject.core.data.repository.HomeRepository
import com.example.fragmject.core.data.repository.HomeRepositoryImpl
import com.example.fragmject.core.data.repository.MyCollectRepository
import com.example.fragmject.core.data.repository.MyCollectRepositoryImpl
import com.example.fragmject.core.data.repository.MyRepository
import com.example.fragmject.core.data.repository.MyRepositoryImpl
import com.example.fragmject.core.data.repository.OfflineFirstArticleRepository
import com.example.fragmject.core.data.repository.OfflineFirstCoinRankRepository
import com.example.fragmject.core.data.repository.OfflineFirstCommonRepository
import com.example.fragmject.core.data.repository.OfflineFirstMyCollectRepository
import com.example.fragmject.core.data.repository.OfflineFirstProjectRepository
import com.example.fragmject.core.data.repository.OfflineFirstSystemRepository
import com.example.fragmject.core.data.repository.ProjectRepository
import com.example.fragmject.core.data.repository.ProjectRepositoryImpl
import com.example.fragmject.core.data.repository.SearchRepository
import com.example.fragmject.core.data.repository.SearchRepositoryImpl
import com.example.fragmject.core.data.repository.SystemRepository
import com.example.fragmject.core.data.repository.SystemRepositoryImpl
import com.example.fragmject.core.data.repository.UserCenterRepository
import com.example.fragmject.core.data.repository.UserCenterRepositoryImpl
import com.example.fragmject.core.data.repository.UserRepository
import com.example.fragmject.core.data.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideArticleRepository(): ArticleRepository = ArticleRepositoryImpl()

    @Provides
    @Singleton
    fun provideProjectRepository(): ProjectRepository = ProjectRepositoryImpl()

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = UserRepositoryImpl()

    @Provides
    @Singleton
    fun provideMyRepository(): MyRepository = MyRepositoryImpl()

    @Provides
    @Singleton
    fun provideCommonRepository(): CommonRepository = CommonRepositoryImpl()

    @Provides
    @Singleton
    fun provideOfflineFirstCommonRepository(
        commonRepository: CommonRepository,
    ): OfflineFirstCommonRepository = OfflineFirstCommonRepository(commonRepository)

    @Provides
    @Singleton
    fun provideOfflineFirstArticleRepository(
        articleRepository: ArticleRepository,
    ): OfflineFirstArticleRepository = OfflineFirstArticleRepository(
        articleDao = AppDatabase.getArticleDao(),
        articleRepo = articleRepository,
    )

    @Provides
    @Singleton
    fun provideOfflineFirstProjectRepository(
        projectRepository: ProjectRepository,
    ): OfflineFirstProjectRepository = OfflineFirstProjectRepository(projectRepository)

    @Provides
    @Singleton
    fun provideOfflineFirstCoinRankRepository(
        commonRepository: CommonRepository,
    ): OfflineFirstCoinRankRepository = OfflineFirstCoinRankRepository(commonRepository)

    @Provides
    @Singleton
    fun provideOfflineFirstSystemRepository(
        articleRepository: ArticleRepository,
    ): OfflineFirstSystemRepository = OfflineFirstSystemRepository(articleRepository)

    @Provides
    @Singleton
    fun provideSystemRepository(
        offlineFirst: OfflineFirstSystemRepository,
    ): SystemRepository = SystemRepositoryImpl(offlineFirst)

    @Provides
    @Singleton
    fun provideOfflineFirstMyCollectRepository(
        articleRepository: ArticleRepository,
    ): OfflineFirstMyCollectRepository = OfflineFirstMyCollectRepository(articleRepository)

    @Provides
    @Singleton
    fun provideMyCollectRepository(
        offlineFirst: OfflineFirstMyCollectRepository,
    ): MyCollectRepository = MyCollectRepositoryImpl(offlineFirst)

    @Provides
    @Singleton
    fun provideHomeRepository(
        offlineFirst: OfflineFirstArticleRepository,
    ): HomeRepository = HomeRepositoryImpl(offlineFirst)

    @Provides
    @Singleton
    fun provideSearchRepository(
        articleRepository: ArticleRepository,
    ): SearchRepository = SearchRepositoryImpl(articleRepository)

    @Provides
    @Singleton
    fun provideUserCenterRepository(
        userRepository: UserRepository,
    ): UserCenterRepository = UserCenterRepositoryImpl(userRepository)
}
