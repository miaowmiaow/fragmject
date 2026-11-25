package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import com.example.fragmject.core.domain.text.FontScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * KV 适配：缺键回退默认值、越界夹紧、写入前夹紧。
 *
 * 缺键是最容易踩的坑——KV 首次读取返回空串，`toFloatOrNull()` 为 null，
 * 若不回退默认值会得到 0sp，界面文字直接消失。
 */
class FontScaleRepositoryImplTest {

    private class FakeKeyValue(
        val stored: MutableMap<String, String> = mutableMapOf(),
    ) : KeyValueLocalDataSource {
        override fun observeValue(key: String): Flow<String> = flowOf(stored[key].orEmpty())
        override suspend fun setValue(key: String, value: String): Boolean {
            stored[key] = value
            return true
        }
    }

    @Test
    fun `缺键回退默认字号`() = runBlocking {
        val repository = FontScaleRepositoryImpl(FakeKeyValue())
        assertEquals(FontScale.DEFAULT_BODY_SP, repository.observeBodyFontSp().first(), 0.001f)
    }

    @Test
    fun `读取时越界值被夹到上限`() = runBlocking {
        val repository = FontScaleRepositoryImpl(FakeKeyValue(mutableMapOf("body_font_sp" to "99")))
        assertEquals(FontScale.MAX_BODY_SP, repository.observeBodyFontSp().first(), 0.001f)
    }

    @Test
    fun `读取时非法值回退默认`() = runBlocking {
        val repository = FontScaleRepositoryImpl(FakeKeyValue(mutableMapOf("body_font_sp" to "abc")))
        assertEquals(FontScale.DEFAULT_BODY_SP, repository.observeBodyFontSp().first(), 0.001f)
    }

    @Test
    fun `写入前夹紧再持久化`() = runBlocking {
        val kv = FakeKeyValue()
        FontScaleRepositoryImpl(kv).setBodyFontSp(100f)
        assertEquals(FontScale.MAX_BODY_SP.toString(), kv.stored["body_font_sp"])
    }
}
