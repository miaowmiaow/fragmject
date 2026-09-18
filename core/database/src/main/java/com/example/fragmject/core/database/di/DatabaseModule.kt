package com.example.fragmject.core.database.di

import android.content.Context
import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.KVDatabase
import com.example.fragmject.core.database.dao.ArticleDao
import com.example.fragmject.core.database.dao.CoinRankDao
import com.example.fragmject.core.database.dao.HistoryDao
import com.example.fragmject.core.database.dao.HotKeyDao
import com.example.fragmject.core.database.dao.KVDao
import com.example.fragmject.core.database.dao.NavigationDao
import com.example.fragmject.core.database.dao.ProjectTreeDao
import com.example.fragmject.core.database.dao.TreeDao
import com.example.fragmject.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 持久层 Hilt 绑定。
 *
 * 取代原 AppDatabase / KVDatabase 内部的双检锁静态单例与 `getXxxDao()` 静态方法，
 * 让 DAO 与数据库实例统一由 Hilt 管理生命周期，从而：
 * - Repository / Store 通过构造函数注入 DAO，可测试性大幅提升；
 * - 测试中可用 `Room.inMemoryDatabaseBuilder` 替换真实数据库。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    @Singleton
    fun provideKVDatabase(@ApplicationContext context: Context): KVDatabase =
        KVDatabase.build(context)

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideTreeDao(db: AppDatabase): TreeDao = db.treeDao()

    @Provides
    fun provideNavigationDao(db: AppDatabase): NavigationDao = db.navigationDao()

    @Provides
    fun provideHotKeyDao(db: AppDatabase): HotKeyDao = db.hotKeyDao()

    @Provides
    fun provideProjectTreeDao(db: AppDatabase): ProjectTreeDao = db.projectTreeDao()

    @Provides
    fun provideCoinRankDao(db: AppDatabase): CoinRankDao = db.coinRankDao()

    @Provides
    fun provideKVDao(db: KVDatabase): KVDao = db.kvDao()
}
