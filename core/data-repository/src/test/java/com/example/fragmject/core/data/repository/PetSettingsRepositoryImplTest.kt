package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.KeyValueLocalDataSource
import com.example.fragmject.core.domain.pet.PetSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PetSettingsRepositoryImpl] 的映射契约。
 *
 * 重点覆盖「KV 缺键 → 默认值」这条最容易踩的路径：`"".toBoolean()` 是 `false`、
 * `"".toFloatOrNull()` 是 `null`，写成 `toBoolean()` 会得到「宠物默认关闭」，
 * 写成 `?: 0f` 会得到「全球面透明」，两种都表现为「功能坏了」。
 */
class PetSettingsRepositoryImplTest {

    private val epsilon = 0.0001f

    @Test
    fun `KV 全空时返回默认值（宠物默认开启）`() = runTest {
        val repository = PetSettingsRepositoryImpl(FakeLocalDataSource())
        assertEquals(PetSettings.Default, repository.observePetSettings().first())
    }

    @Test
    fun `缺部分键时逐字段回退默认值`() = runTest {
        val local = FakeLocalDataSource(mapOf("pet_opacity" to "0.4"))
        val settings = PetSettingsRepositoryImpl(local).observePetSettings().first()
        assertEquals(0.4f, settings.opacity, epsilon)
        assertEquals(PetSettings.Default.sizeScale, settings.sizeScale, epsilon)
        assertEquals(PetSettings.Default.enabled, settings.enabled)
    }

    @Test
    fun `脏值被忽略并夹紧`() = runTest {
        val local = FakeLocalDataSource(
            mapOf(
                "pet_enabled" to "not-a-boolean",
                "pet_opacity" to "999",
                "pet_size_scale" to "abc",
                "pet_speed_scale" to "-5",
            ),
        )
        val settings = PetSettingsRepositoryImpl(local).observePetSettings().first()
        assertEquals(PetSettings.Default.enabled, settings.enabled)
        assertEquals(PetSettings.MAX_OPACITY, settings.opacity, epsilon)
        assertEquals(PetSettings.Default.sizeScale, settings.sizeScale, epsilon)
        assertEquals(PetSettings.MIN_SPEED_SCALE, settings.speedScale, epsilon)
    }

    @Test
    fun `写入前夹紧并且落库为字符串`() = runTest {
        val local = FakeLocalDataSource()
        val repository = PetSettingsRepositoryImpl(local)

        repository.setOpacity(9f)
        repository.setSizeScale(0.01f)
        repository.setSpeedScale(3f)
        repository.setEnabled(false)

        assertEquals("${PetSettings.MAX_OPACITY}", local.values["pet_opacity"])
        assertEquals("${PetSettings.MIN_SIZE_SCALE}", local.values["pet_size_scale"])
        assertEquals("${PetSettings.MAX_SPEED_SCALE}", local.values["pet_speed_scale"])
        assertEquals("false", local.values["pet_enabled"])
        assertTrue(local.values.keys.all { it.startsWith("pet_") })
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
