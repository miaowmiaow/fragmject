package com.example.fragmject.core.webview.di

import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.webview.WebViewCookieStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Cookie 存储绑定：把 WebView 实现绑定到 core:domain 的 [CookieStore] 契约。
 *
 * 使 core:network 只依赖领域契约即可复用 WebView 的登录会话，
 * 无需（也不允许）反向依赖 core:webview。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CookieStoreModule {

    @Binds
    @Singleton
    abstract fun bindCookieStore(impl: WebViewCookieStore): CookieStore
}
