package com.example.fragmject.feature.user.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.domain.repository.PetSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 漂浮宠物配置页的 ViewModel。
 *
 * 写入策略：**每个滑杆各写各的**（`setOpacity` / `setSizeScale` / `setSpeedScale`），
 * 不做「整份 settings 覆盖保存」。原因是滑杆提交是异步的，若整份覆盖，
 * 两次快速拖动会互相把对方的旧值写回去。
 *
 * 读侧统一走 [PetSettingsRepository] 的 Flow：仓库已负责缺键默认值与范围夹紧，
 * 这里不再判空/夹紧，UI 拿到的永远是合法值。
 */
@HiltViewModel
class PetSettingViewModel @Inject constructor(
    private val repository: PetSettingsRepository,
) : ViewModel() {

    val settings: StateFlow<PetSettings> = repository.observePetSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS),
            initialValue = PetSettings.Default,
        )

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(enabled) }
    }

    fun setOpacity(opacity: Float) {
        viewModelScope.launch { repository.setOpacity(opacity) }
    }

    fun setSizeScale(scale: Float) {
        viewModelScope.launch { repository.setSizeScale(scale) }
    }

    fun setSpeedScale(scale: Float) {
        viewModelScope.launch { repository.setSpeedScale(scale) }
    }

    private companion object {
        /** 与项目其它页一致：短暂离开再回来不重建订阅，避免滑杆位置回跳。 */
        const val SUBSCRIBE_TIMEOUT_MS = 5_000L
    }
}
