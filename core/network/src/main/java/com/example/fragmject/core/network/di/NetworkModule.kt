package com.example.fragmject.core.network.di

import android.content.Context
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource
import com.example.fragmject.core.data.contract.remote.ProjectRemoteDataSource
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.network.BuildConfig
import com.example.fragmject.core.network.OkHttpFileDownloader
import com.example.fragmject.core.network.http.AssetsFallbackInterceptor
import com.example.fragmject.core.network.http.GsonUtils
import com.example.fragmject.core.network.http.OkHttpClients
import com.example.fragmject.core.network.service.ArticleService
import com.example.fragmject.core.network.service.CommonService
import com.example.fragmject.core.network.service.MyService
import com.example.fragmject.core.network.service.ProjectService
import com.example.fragmject.core.network.service.UserService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/** 下载专用 OkHttpClient 的 qualifier。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadOkHttpClient

/** 图片加载（Coil）专用 OkHttpClient 的 qualifier。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageOkHttpClient

/**
 * 网络层 Hilt 模块：集中提供 OkHttpClient、Retrofit、下载器与各数据源。
 *
 * client 一律经 [OkHttpClients] 构建并以 @Singleton 持有，取代原 OkUtils 的静态缓存。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAssetsFallbackInterceptor(
        @ApplicationContext context: Context,
    ): AssetsFallbackInterceptor = AssetsFallbackInterceptor(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        clients: OkHttpClients,
        fallbackInterceptor: AssetsFallbackInterceptor,
    ): OkHttpClient = clients.http().newBuilder()
        .addInterceptor(fallbackInterceptor)
        .build()

    @Provides
    @Singleton
    @DownloadOkHttpClient
    fun provideDownloadClient(clients: OkHttpClients): OkHttpClient = clients.noCache()

    @Provides
    @Singleton
    @ImageOkHttpClient
    fun provideImageClient(clients: OkHttpClients): OkHttpClient = clients.noCache()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonUtils.gson))
        .build()

    @Provides
    @Singleton
    fun provideDownloader(
        @DownloadOkHttpClient client: OkHttpClient,
    ): DownloadRemoteDataSource = OkHttpFileDownloader(client)

    @Provides
    @Singleton
    fun provideArticleDataSource(retrofit: Retrofit): ArticleRemoteDataSource =
        retrofit.create(ArticleService::class.java)

    @Provides
    @Singleton
    fun provideProjectDataSource(retrofit: Retrofit): ProjectRemoteDataSource =
        retrofit.create(ProjectService::class.java)

    @Provides
    @Singleton
    fun provideCommonDataSource(retrofit: Retrofit): CommonRemoteDataSource =
        retrofit.create(CommonService::class.java)

    @Provides
    @Singleton
    fun provideUserRemoteDataSource(retrofit: Retrofit): UserRemoteDataSource =
        retrofit.create(UserService::class.java)

    @Provides
    @Singleton
    fun provideMyRemoteDataSource(retrofit: Retrofit): MyRemoteDataSource =
        retrofit.create(MyService::class.java)
}

/**
 * 供无法构造注入的场景（Coil ImageLoaderFactory）获取图片专用 client 的入口。
 *
 * 与 core:player 的 ExoPlayerCacheEntryPoint 手法一致。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkImageEntryPoint {
    @ImageOkHttpClient
    fun imageClient(): OkHttpClient
}

/** 从 ApplicationContext 获取图片专用 client。 */
fun imageOkHttpClient(context: Context): OkHttpClient =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        NetworkImageEntryPoint::class.java,
    ).imageClient()
