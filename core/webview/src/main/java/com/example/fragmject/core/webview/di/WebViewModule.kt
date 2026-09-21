package com.example.fragmject.core.webview.di

import com.example.fragmject.core.webview.WebViewPool
import com.example.fragmject.core.webview.WebViewManager
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WebView 池 Hilt 绑定：把 [WebViewManager] 单例绑定到 core:webview 的 [WebViewPool] 契约，
 * 使消费方只依赖接口而非具体实现。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WebViewModule {

    @Binds
    @Singleton
    abstract fun bindWebViewPool(impl: WebViewManager): WebViewPool
}

/**
 * 供 Compose 组件（WebView.kt）获取 [WebViewPool] 契约实例的 Hilt EntryPoint。
 *
 * Composable 无法直接使用 @Inject 构造注入，故通过 [dagger.hilt.android.EntryPointAccessors]
 * 从 ApplicationContext 获取已由 Hilt 管理的单例；这里暴露接口而非 WebViewManager 实现，
 * 使 WebView 组件只依赖 core:webview 契约，进一步切断对具体实现的耦合。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WebViewPoolEntryPoint {
    fun webViewPool(): WebViewPool
}
