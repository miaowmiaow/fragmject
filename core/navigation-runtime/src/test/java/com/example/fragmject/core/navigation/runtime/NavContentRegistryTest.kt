package com.example.fragmject.core.navigation.runtime

import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavContentRegistryTest {

    private data object Home : NavKey
    private data object Detail : NavKey

    @Test
    fun `forEach 按注册顺序遍历全部 NavKey`() {
        val registry = NavContentRegistry()
        registry.register<Home> { }
        registry.register<Detail> { }

        val seen = mutableListOf<KClass<out NavKey>>()
        registry.forEach { clazz, _ -> seen += clazz }

        assertEquals(listOf(Home::class, Detail::class), seen)
    }

    @Test
    fun `同一 NavKey 重复注册即失败，避免静默覆盖`() {
        val registry = NavContentRegistry()
        registry.register<Home> { }

        val thrown = runCatching { registry.register<Home> { } }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertTrue(thrown?.message.orEmpty().contains("Duplicate NavKey registration"))
    }

    @Test
    fun `未注册的 NavKey 不会出现在遍历结果中`() {
        val registry = NavContentRegistry()
        registry.register<Home> { }

        val seen = mutableListOf<KClass<out NavKey>>()
        registry.forEach { clazz, _ -> seen += clazz }

        assertEquals(listOf(Home::class), seen)
        assertTrue(Detail::class !in seen)
    }
}
