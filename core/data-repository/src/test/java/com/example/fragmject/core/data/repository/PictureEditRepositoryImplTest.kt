package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [PictureEditRepositoryImpl] 的读写契约。
 *
 * 重点是「未记录返回 null」与「同一条旧 uri 以最后一次写入为准」：
 * 前者决定预览是否回退到原始 uri，后者决定连续编辑时取到的是最新一跳。
 */
class PictureEditRepositoryImplTest {

    @Test
    fun `没有记录时返回 null`() = runTest {
        val repository = PictureEditRepositoryImpl(FakeLocalDataSource())
        assertNull(repository.findReplacement("content://media/1"))
    }

    @Test
    fun `记录后可查到新 uri`() = runTest {
        val repository = PictureEditRepositoryImpl(FakeLocalDataSource())
        repository.recordReplacement("content://media/1", "content://media/2")
        assertEquals("content://media/2", repository.findReplacement("content://media/1"))
    }

    @Test
    fun `同一旧 uri 以最后一次写入为准`() = runTest {
        val repository = PictureEditRepositoryImpl(FakeLocalDataSource())
        repository.recordReplacement("content://media/1", "content://media/2")
        repository.recordReplacement("content://media/1", "content://media/3")
        assertEquals("content://media/3", repository.findReplacement("content://media/1"))
    }

    @Test
    fun `kv 值为空串视为没有记录`() = runTest {
        val local = FakeLocalDataSource(mapOf("picture_edit_replacement:content://media/1" to ""))
        assertNull(PictureEditRepositoryImpl(local).findReplacement("content://media/1"))
    }

    @Test
    fun `业务键带前缀，不与设置项混用`() = runTest {
        val local = FakeLocalDataSource()
        PictureEditRepositoryImpl(local).recordReplacement("content://media/1", "content://media/2")
        assertEquals(setOf("picture_edit_replacement:content://media/1"), local.values.keys)
    }

    /** 内存版本地端口：按 key 存字符串，模拟 KV 的「缺键返回空串」语义。 */
    private class FakeLocalDataSource(
        initial: Map<String, String> = emptyMap(),
    ) : KeyValueLocalDataSource {
        val values = initial.toMutableMap()

        override fun observeValue(key: String): Flow<String> =
            MutableStateFlow(values.toMap()).map { it[key] ?: "" }

        override suspend fun setValue(key: String, value: String) {
            values[key] = value
        }
    }
}
