package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.pet.PetSettings
import kotlinx.coroutines.flow.Flow

/**
 * 漂浮宠物设置的领域端口。
 *
 * 只暴露 [Flow] 供设置页与 `:app` 观察，写入按「一个字段一个方法」拆分：
 * 设置页每次只改一个滑杆，合并成 `save(settings)` 反而会让并发写入互相覆盖。
 *
 * 实现负责「缺键取默认值 + 夹紧范围」，调用方拿到的 [PetSettings] 一定合法。
 */
interface PetSettingsRepository {
    fun observePetSettings(): Flow<PetSettings>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setOpacity(opacity: Float)

    suspend fun setSizeScale(scale: Float)

    suspend fun setSpeedScale(scale: Float)
}
