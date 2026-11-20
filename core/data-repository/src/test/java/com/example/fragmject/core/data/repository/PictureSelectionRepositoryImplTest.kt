package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [PictureSelectionRepositoryImpl] 的编码契约。
 *
 * 重点是「同一批记录顺序不变」与「空列表等价于清空」：选中顺序决定选择器上的序号角标，
 * 而清空是每次新流程进入选择器时的归零动作，必须真的把记录抹掉。
 */
class PictureSelectionRepositoryImplTest {

    @Test
    fun `没有记录时返回空列表`() = runTest {
        assertEquals(emptyList<String>(), PictureSelectionRepositoryImpl(FakeLocalDataSource()).loadSelectedUris())
    }

    @Test
    fun `保存后按原顺序读回`() = runTest {
        val repository = PictureSelectionRepositoryImpl(FakeLocalDataSource())
        val uris = listOf(
            "content://media/external/images/media/1000000001",
            "content://media/external/images/media/1000000002",
            "content://media/external/images/media/1000000003",
        )
        repository.saveSelectedUris(uris)
        assertEquals(uris, repository.loadSelectedUris())
    }

    @Test
    fun `保存空列表等价于清空`() = runTest {
        val repository = PictureSelectionRepositoryImpl(FakeLocalDataSource())
        repository.saveSelectedUris(listOf("content://media/external/images/media/1"))
        repository.saveSelectedUris(emptyList())
        assertEquals(emptyList<String>(), repository.loadSelectedUris())
    }

    @Test
    fun `多余空行被忽略`() = runTest {
        val local = FakeLocalDataSource(mapOf("picture_selected_uris" to "\ncontent://media/a\n\ncontent://media/b\n"))
        assertEquals(
            listOf("content://media/a", "content://media/b"),
            PictureSelectionRepositoryImpl(local).loadSelectedUris(),
        )
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
