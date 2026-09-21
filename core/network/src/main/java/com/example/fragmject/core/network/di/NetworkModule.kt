package com.example.fragmject.core.network.di

import android.content.Context
import com.example.fragmject.core.network.http.GsonUtils
import com.example.fragmject.core.network.Downloader
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import com.example.fragmject.core.data.contract.remote.DownloadRemoteDataSource
import com.example.fragmject.core.data.contract.remote.MyRemoteDataSource
import com.example.fragmject.core.data.contract.remote.ProjectRemoteDataSource
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.network.http.AssetsFallbackInterceptor
import com.example.fragmject.core.network.http.OkUtils
import com.example.fragmject.core.network.service.ArticleService
import com.example.fragmject.core.network.service.CommonService
import com.example.fragmject.core.network.service.MyService
import com.example.fragmject.core.network.service.ProjectService
import com.example.fragmject.core.network.service.UserService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
annotation class CoilOkHttpClient

/**
 * 网络层 Hilt 模块：集中提供 OkHttpClient、Retrofit、下载器与各数据源。
 *
 * 取代原 CoroutineHttp 全局单例，
 * Retrofit 注解接口以 internal 封装在本模块内，对外只暴露纯抽象数据源契约。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://www.wanandroid.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
    ): OkHttpClient = OkUtils.httpClient(context).newBuilder()
        .addInterceptor(AssetsFallbackInterceptor(context))
        .build()

    @Provides
    @Singleton
    @CoilOkHttpClient
    fun provideCoilOkHttpClient(
        @ApplicationContext context: Context,
    ): OkHttpClient = OkUtils.httpClient(context)

    @Provides
    @Singleton
    @DownloadOkHttpClient
    fun provideDownloadClient(client: OkHttpClient): OkHttpClient {
        // 下载专用 client：移除兜底拦截器、清除 BODY 日志拦截器、禁用缓存，仅保留 HEADERS 日志。
        return client.newBuilder().apply {
            interceptors().removeAll { it is AssetsFallbackInterceptor }
            networkInterceptors().removeAll { it is HttpLoggingInterceptor }
            addNetworkInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
            )
            cache(null)
        }.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonUtils.gson))
        .build()

    @Provides
    @Singleton
    fun provideDownloader(
        @DownloadOkHttpClient client: OkHttpClient,
    ): DownloadRemoteDataSource = Downloader(client)

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
