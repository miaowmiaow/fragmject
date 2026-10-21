package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.repository.ThemeRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * 主题态契约：Eagerly 订阅仓库，默认浅色，并持续跟随最新值。
 */
class ThemeStateImplTest {

    private class FakeThemeRepository(private val themes: Flow<Boolean>) : ThemeRepository {
        override fun observeDarkTheme(): Flow<Boolean> = themes
        override suspend fun setDarkTheme(dark: Boolean) = error("unused")
    }

    private fun <T> withState(themes: Flow<Boolean>, block: suspend (ThemeStateImpl) -> T): T {
        val scope = AppCoroutineScope()
        try {
            return runBlocking { block(ThemeStateImpl(FakeThemeRepository(themes), scope)) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `defaults to light before the repository emits`() {
        withState(flow { awaitCancellation() }) { state ->
            assertFalse(state.darkTheme.value)
        }
    }

    @Test
    fun `follows the repository dark theme`() {
        withState(flowOf(true)) { state ->
            assertTrue(withTimeout(5_000.milliseconds) { state.darkTheme.first { it } })
        }
    }

    @Test
    fun `keeps the latest emitted value`() {
        val themes = MutableSharedFlow<Boolean>(replay = 1)
        withState(themes) { state ->
            themes.emit(true)
            assertTrue(withTimeout(5_000.milliseconds) { state.darkTheme.first { it } })

            themes.emit(false)
            assertFalse(withTimeout(5_000.milliseconds) { state.darkTheme.first { !it } })
        }
    }
}
