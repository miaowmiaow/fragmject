package com.example.fragmject.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.fragmject.core.android.platform.CacheUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ExoPlayer 播放缓存依赖装配。
 *
 * [SimpleCache] 同目录仅允许一个实例，故以 @Singleton 提供，避免 ExoPlayerContainer
 * 每次组合重建导致的实例冲突，以及 release 后复用同一目录的崩溃时序。
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object ExoPlayerCacheModule {

    @Provides
    @Singleton
    fun provideStandaloneDatabaseProvider(
        @ApplicationContext context: Context,
    ): StandaloneDatabaseProvider = StandaloneDatabaseProvider(context)

    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context,
        databaseProvider: StandaloneDatabaseProvider,
    ): SimpleCache = SimpleCache(
        CacheUtils.getDirFile(context, "exoplayer_cache"),
        LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024),
        databaseProvider,
    )
}

/**
 * 供 Composable 通过 [EntryPointAccessors] 获取单例 [SimpleCache] 的入口。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExoPlayerCacheEntryPoint {
    fun simpleCache(): SimpleCache
}