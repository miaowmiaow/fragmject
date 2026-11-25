package com.example.fragmject.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 界面字体缩放的领域端口，以「正文字号（sp）」为单位。
 *
 * 单位用 sp 而不是倍率：持久化的语义与 UI 展示一致，将来基准字号从 14 调整到 15 时，
 * 历史设置仍然是「用户想要的字号」，不会因为基准变化而整体偏移。
 *
 * 缺键回退 [com.example.fragmject.core.domain.text.FontScale.DEFAULT_BODY_SP]、越界夹紧，
 * 都由实现（`core:data-repository`）兜住，UI 侧不必再处理。
 */
interface FontScaleRepository {

    fun observeBodyFontSp(): Flow<Float>

    suspend fun setBodyFontSp(sp: Float)
}
