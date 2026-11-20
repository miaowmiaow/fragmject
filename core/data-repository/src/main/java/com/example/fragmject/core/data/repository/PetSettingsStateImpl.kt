package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.domain.pet.PetSettingsState
import com.example.fragmject.core.domain.repository.PetSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 宠物设置态实现：把 [PetSettingsRepository.observePetSettings] 提升为应用级状态。
 *
 * 与 [ThemeStateImpl] 同一套路：`SharingStarted.Eagerly` + 应用作用域，进程存活期间一直持值。
 * 这样 `MainActivity` 在 Activity 重建后首帧就能拿到真实值，不再出现「鱼先消失一帧」。
 *
 * 初值取 [PetSettings.Default]（关闭）：冷启动时 Room 还没回值，先按关闭处理——
 * 此时鱼本来就不在屏幕上，晚几帧出现不会表现为闪烁（见 `PetSettingsState` 的已知限制）。
 */
@Singleton
class PetSettingsStateImpl @Inject constructor(
    petSettingsRepository: PetSettingsRepository,
    appScope: AppCoroutineScope,
) : PetSettingsState {
    override val settings: StateFlow<PetSettings> =
        petSettingsRepository.observePetSettings()
            .stateIn(appScope, SharingStarted.Eagerly, PetSettings.Default)
}
