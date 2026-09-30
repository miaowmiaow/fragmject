package com.example.fragmject.core.data.repository.di

import com.example.fragmject.core.android.platform.FileDownloader
import com.example.fragmject.core.domain.repository.DownloadRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 平台能力端口桥接：把 core:domain 的 [DownloadRepository] 适配成
 * android-platform 的 [FileDownloader]，供 WebView 资源缓存复用。
 *
 * 单独成 object Module（@Provides 工厂）与 [RepositoryModule]（@Binds 抽象绑定）分离，
 * 保持两种绑定形态各司其职。
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloaderBridgeModule {

    @Provides
    @Singleton
    fun provideFileDownloader(
        downloadRepository: DownloadRepository,
    ): FileDownloader = FileDownloader { url, savePath, fileName, headers ->
        downloadRepository.download(url, savePath, fileName, headers).success
    }
}
