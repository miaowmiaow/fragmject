package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.repository.FontScaleRepository
import com.example.fragmject.core.domain.text.FontScale
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 字体缩放态：Eagerly 订阅仓库，默认 14sp，并持续跟随最新值。
 *
 * 与 `ThemeStateImplTest` 同一套路；默认值这一点尤其重要——`MainActivity` 用它做
 * `initialValue`，如果默认值不是 14sp，Activity 每次重建都会闪一下错误字号。
 */
class FontScaleStateImplTest {

    private class FakeFontScaleRepository(private val sp: Flow<Float>) : FontScaleRepository {
        override fun observeBodyFontSp(): Flow<Float> = sp
        override suspend fun setBodyFontSp(sp: Float) = error("unused")
    }

    private fun <T> withState(sp: Flow<Float>, block: suspend (FontScaleStateImpl) -> T): T {
        val scope = AppCoroutineScope()
        try {
            return runBlocking { block(FontScaleStateImpl(FakeFontScaleRepository(sp), scope)) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `仓库未发射时使用默认字号`() = withState(flow { awaitCancellation() }) { state ->
        assertEquals(FontScale.DEFAULT_BODY_SP, state.bodyFontSp.value, 0.001f)
    }

    @Test
    fun `跟随仓库发射的最新字号`() = withState(flowOf(17f)) { state ->
        val sp = withTimeout(5_000) { state.bodyFontSp.first { it == 17f } }
        assertEquals(17f, sp, 0.001f)
    }
}
