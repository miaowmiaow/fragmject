package com.example.fragmject.core.webview

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

/**
 * [WebCacheSettings] 的 SharedPreferences 实现。
 *
 * 用独立的 prefs 文件而不是并入 KV 数据库：这个开关要在 WebView 拦截链路（可能早于数据库初始化）
 * 里被同步读取，`SharedPreferences` 首次读取是同步磁盘 IO，但只有一项、代价可忽略。
 *
 * 默认值 **true**：缓存的目标是体验，默认开启；风险由「带凭据条目仅同源命中」兜住，
 * 用户可在设置页关闭。
 */
@Singleton
class SharedPreferencesWebCacheSettings @Inject constructor(
    @ApplicationContext context: Context,
) : WebCacheSettings {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _credentialedCachingEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_CREDENTIALED_CACHING, DEFAULT_CREDENTIALED_CACHING))

    override val credentialedCachingEnabled: StateFlow<Boolean> = _credentialedCachingEnabled.asStateFlow()

    private val _credentialedNotice = MutableStateFlow(false)

    override val credentialedNotice: StateFlow<Boolean> = _credentialedNotice.asStateFlow()

    override fun setCredentialedCachingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CREDENTIALED_CACHING, enabled) }
        _credentialedCachingEnabled.value = enabled
    }

    override fun consumeCredentialedNotice() {
        _credentialedNotice.value = false
    }

    override fun notifyCredentialedCacheUsed() {
        _credentialedNotice.value = true
    }

    private companion object {
        const val PREFS_NAME = "web_cache_settings"
        const val KEY_CREDENTIALED_CACHING = "credentialed_caching_enabled"
        const val DEFAULT_CREDENTIALED_CACHING = true
    }
}
