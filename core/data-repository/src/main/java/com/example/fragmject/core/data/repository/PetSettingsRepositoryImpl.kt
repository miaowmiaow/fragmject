package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.domain.repository.PetSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PetSettingsRepository] 的适配器实现：把 4 个 KV 字段映射成一个 [PetSettings]。
 *
 * 两条不变量都在这里兜住，UI 侧不需要再判空/夹紧：
 * 1. **缺键取默认值**：KV 首次读取返回空串，直接 `toBoolean()`/`toFloatOrNull()` 会得到
 *    `false` / `null`，表现为「宠物默认关闭、透明度 0」——必须显式回退；
 * 2. **范围夹紧**：见 [PetSettings.sanitized]。
 */
@Singleton
class PetSettingsRepositoryImpl @Inject constructor(
    private val local: KeyValueLocalDataSource,
) : PetSettingsRepository {

    override fun observePetSettings(): Flow<PetSettings> = combine(
        local.observeValue(KEY_ENABLED),
        local.observeValue(KEY_OPACITY),
        local.observeValue(KEY_SIZE_SCALE),
        local.observeValue(KEY_SPEED_SCALE),
    ) { enabled, opacity, sizeScale, speedScale ->
        PetSettings(
            enabled = enabled.toBooleanStrictOrNull() ?: PetSettings.Default.enabled,
            opacity = opacity.toFloatOrNull() ?: PetSettings.Default.opacity,
            sizeScale = sizeScale.toFloatOrNull() ?: PetSettings.Default.sizeScale,
            speedScale = speedScale.toFloatOrNull() ?: PetSettings.Default.speedScale,
        ).sanitized()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        local.setValue(KEY_ENABLED, enabled.toString())
    }

    override suspend fun setOpacity(opacity: Float) {
        local.setValue(KEY_OPACITY, opacity.coerceIn(PetSettings.MIN_OPACITY, PetSettings.MAX_OPACITY).toString())
    }

    override suspend fun setSizeScale(scale: Float) {
        local.setValue(KEY_SIZE_SCALE, scale.coerceIn(PetSettings.MIN_SIZE_SCALE, PetSettings.MAX_SIZE_SCALE).toString())
    }

    override suspend fun setSpeedScale(scale: Float) {
        local.setValue(
            KEY_SPEED_SCALE,
            scale.coerceIn(PetSettings.MIN_SPEED_SCALE, PetSettings.MAX_SPEED_SCALE).toString(),
        )
    }

    private companion object {
        const val KEY_ENABLED = "pet_enabled"
        const val KEY_OPACITY = "pet_opacity"
        const val KEY_SIZE_SCALE = "pet_size_scale"
        const val KEY_SPEED_SCALE = "pet_speed_scale"
    }
}
