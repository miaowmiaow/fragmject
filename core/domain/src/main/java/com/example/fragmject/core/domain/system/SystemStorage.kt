package com.example.fragmject.core.domain.system

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 设备存储 / 缓存领域端口。
 *
 * 「缓存清理范围」是业务规则（例如下载成品目录不参与清理），因此收敛在实现内；
 * UI 只消费展示用的大小文本与结果，不接触缓存目录本身。
 */
interface SystemStorage {
    /** 供展示的缓存大小文本（如 "12.4MB"）。 */
    val cacheSizeText: StateFlow<String>

    /** 是否正在清理，用于禁用按钮与显示进度。 */
    val isClearing: StateFlow<Boolean>

    /**
     * 供第三方组件（如 Coil 磁盘缓存）使用的缓存根目录。
     *
     * 目录策略属业务规则（是否可被清缓存影响、是否放外部存储），由实现决定；
     * 调用方不接触缓存路径，避免平台工具类散落到组合根与 UI。
     */
    fun cacheDirectory(): File

    suspend fun refreshCacheSize()

    /** 清理缓存并刷新大小；失败返回 [Result.failure]，[isClearing] 必定复位。 */
    suspend fun clearCache(): Result<Unit>
}
