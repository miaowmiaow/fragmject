package com.example.fragmject.core.webview.di

import com.example.fragmject.core.domain.session.SessionCacheCleaner
import com.example.fragmject.core.webview.SharedPreferencesWebCacheSettings
import com.example.fragmject.core.webview.WebCacheSettings
import com.example.fragmject.core.webview.WebResourceCache
import com.example.fragmject.core.webview.WebResourceCacheManager
import com.example.fragmject.core.webview.WebSessionCacheCleaner
import com.example.fragmject.core.webview.WebViewPool
import com.example.fragmject.core.webview.WebViewPoolManager
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WebView Hilt 绑定：
 * - 把 [WebViewPoolManager] 单例绑定到 core:webview 的 [WebViewPool] 契约（池生命周期）；
 * - 把 [WebResourceCacheManager] 单例绑定到 [WebResourceCache] 契约（资源缓存与加载优化）；
 * - 把 [WebSessionCacheCleaner] 绑定到 core:domain 的 [SessionCacheCleaner] 契约
 *   （登出/换号时清残留缓存，由 `AuthSessionCoordinator` 调用）。
 * 使消费方只依赖接口而非具体实现。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WebViewModule {

    @Binds
    @Singleton
    abstract fun bindWebViewPool(impl: WebViewPoolManager): WebViewPool

    @Binds
    @Singleton
    abstract fun bindWebResourceCache(impl: WebResourceCacheManager): WebResourceCache

    @Binds
    @Singleton
    abstract fun bindSessionCacheCleaner(impl: WebSessionCacheCleaner): SessionCacheCleaner

    @Binds
    @Singleton
    abstract fun bindWebCacheSettings(impl: SharedPreferencesWebCacheSettings): WebCacheSettings
}

/**
 * 供 Compose 组件（WebViewContainer.kt / WebScreen.kt）获取契约实例的 Hilt EntryPoint。
 *
 * Composable 无法直接使用 @Inject 构造注入，故通过 [dagger.hilt.android.EntryPointAccessors]
 * 从 ApplicationContext 获取已由 Hilt 管理的单例；这里暴露接口而非具体实现，
 * 使 WebView 组件只依赖 core:webview 契约，进一步切断对具体实现的耦合。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WebViewPoolEntryPoint {
    fun webViewPool(): WebViewPool

    fun webResourceCache(): WebResourceCache

    /** 缓存策略设置（设置页开关与首次使用提示）。 */
    fun webCacheSettings(): WebCacheSettings
}
