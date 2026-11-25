package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import com.example.fragmject.core.domain.repository.FontScaleRepository
import com.example.fragmject.core.domain.text.FontScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FontScaleRepository] 的 KV 适配器实现。
 *
 * 与 [PetSettingsRepositoryImpl] 同一套路：字段语义（缺键回退、越界夹紧）在这一层兜住，
 * `core:database` 只做原始字符串读写，不必为它开「直接实现 domain 端口」的白名单例外。
 */
@Singleton
class FontScaleRepositoryImpl @Inject constructor(
    private val local: KeyValueLocalDataSource,
) : FontScaleRepository {

    override fun observeBodyFontSp(): Flow<Float> =
        local.observeValue(KEY_BODY_FONT_SP).map { raw ->
            // 首次读取是空串：toFloatOrNull() 为 null，必须回退默认值，否则会得到 0sp
            raw.toFloatOrNull()?.let(FontScale::clampBodySp) ?: FontScale.DEFAULT_BODY_SP
        }

    override suspend fun setBodyFontSp(sp: Float) {
        local.setValue(KEY_BODY_FONT_SP, FontScale.clampBodySp(sp).toString())
    }

    private companion object {
        const val KEY_BODY_FONT_SP = "body_font_sp"
    }
}
