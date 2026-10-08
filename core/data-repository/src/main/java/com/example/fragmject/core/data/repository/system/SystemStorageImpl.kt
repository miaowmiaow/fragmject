package com.example.fragmject.core.data.repository.system

import android.content.Context
import com.example.fragmject.core.android.platform.cache.CacheDirs
import com.example.fragmject.core.android.platform.cache.CacheUtils
import com.example.fragmject.core.domain.system.SystemStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SystemStorage] 的 data 层实现。
 *
 * 缓存目录与清理范围等业务规则收敛在此，UI 不再直接接触 CacheUtils：
 * - 下载成品（应用专属外部文件目录）不在缓存目录内，天然不参与清理；
 * - [CacheDirs.PROTECTED] 中的常驻缓存由进程内单例长期持有，清理时必须排除，
 *   否则会破坏 Coil DiskCache / OkHttp 磁盘缓存 / ExoPlayer SimpleCache 的索引与锁。
 */
@Singleton
class SystemStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SystemStorage {

    private val _cacheSizeText = MutableStateFlow("0KB")
    override val cacheSizeText: StateFlow<String> = _cacheSizeText.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    override val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    // 刻意不在 init 中统计缓存大小：本单例由 :app 组合根注入（用于 Coil 磁盘缓存目录），
    // 会在 Application 阶段被实例化，而 getTotalSize 要递归遍历 cacheDir + externalCacheDir
    // 并对每个文件做两次 stat（web_cache 上限 5000 文件 → 万级 stat），与冷启动首屏抢 IO。
    // 改由消费者按需触发：设置页进入时 SettingViewModel 会调 refreshCacheSize()。

    override fun cacheDirectory(): File = CacheUtils.getDirFile(context, CacheDirs.COIL)

    override suspend fun refreshCacheSize() = withContext(Dispatchers.IO) {
        _cacheSizeText.value = CacheUtils.getTotalSize(context)
    }

    override suspend fun clearCache(): Result<Unit> {
        // 在调用方线程置位，保证 UI 能立即观察到「清理中」；
        // 用 try/finally 兜底：取消（页面退出）或异常时也必须复位，避免按钮永久禁用。
        _isClearing.value = true
        return try {
            withContext(Dispatchers.IO) {
                runCatching {
                    CacheUtils.clearAllCache(context, CacheDirs.PROTECTED)
                    _cacheSizeText.value = CacheUtils.getTotalSize(context)
                }
            }
        } finally {
            _isClearing.value = false
        }
    }

}
