package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.repository.FontScaleRepository
import com.example.fragmject.core.domain.text.FontScale
import com.example.fragmject.core.domain.text.FontScaleState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 字体缩放态实现：把 [FontScaleRepository.observeBodyFontSp] 提升为应用级状态。
 *
 * 与 [ThemeStateImpl]、[PetSettingsStateImpl] 同一套路：`SharingStarted.Eagerly` + 应用作用域，
 * 进程存活期间一直持值，Activity 重建后首帧不会跳回 1.0 倍。
 */
@Singleton
class FontScaleStateImpl @Inject constructor(
    fontScaleRepository: FontScaleRepository,
    appScope: AppCoroutineScope,
) : FontScaleState {

    override val bodyFontSp: StateFlow<Float> =
        fontScaleRepository.observeBodyFontSp()
            .stateIn(appScope, SharingStarted.Eagerly, FontScale.DEFAULT_BODY_SP)
}
