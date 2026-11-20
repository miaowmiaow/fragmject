package com.example.fragmject.core.data.repository

import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.domain.repository.PetSettingsRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * 宠物设置态：Eagerly 订阅仓库，默认关闭，并持续跟随最新值。
 *
 * 覆盖「上游未发值时为初值」与「跟随上游」两种情形——前者正是 Activity 重建首帧读到的东西。
 */
class PetSettingsStateImplTest {

    private class FakePetSettingsRepository(
        private val settings: Flow<PetSettings>,
    ) : PetSettingsRepository {
        override fun observePetSettings(): Flow<PetSettings> = settings
        override suspend fun setEnabled(enabled: Boolean) = error("unused")
        override suspend fun setOpacity(opacity: Float) = error("unused")
        override suspend fun setSizeScale(scale: Float) = error("unused")
        override suspend fun setSpeedScale(scale: Float) = error("unused")
    }

    private fun <T> withState(
        settings: Flow<PetSettings>,
        block: suspend (PetSettingsStateImpl) -> T,
    ): T {
        val scope = AppCoroutineScope()
        try {
            return runBlocking { block(PetSettingsStateImpl(FakePetSettingsRepository(settings), scope)) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `defaults to disabled before the repository emits`() {
        withState(flow { awaitCancellation() }) { state ->
            assertFalse(state.settings.value.enabled)
            assertEquals(PetSettings.Default, state.settings.value)
        }
    }

    @Test
    fun `follows the repository settings`() {
        val enabled = PetSettings(enabled = true, opacity = 0.5f, sizeScale = 1.2f, speedScale = 1.5f)
        withState(flowOf(enabled)) { state ->
            assertTrue(withTimeout(5_000.milliseconds) { state.settings.first { it.enabled }.enabled })
            assertEquals(enabled, state.settings.value)
        }
    }

    @Test
    fun `keeps the latest emitted value`() {
        val updates = MutableSharedFlow<PetSettings>(replay = 1)
        withState(updates) { state ->
            updates.emit(PetSettings(enabled = true, opacity = 0.3f))
            assertEquals(
                0.3f,
                withTimeout(5_000.milliseconds) { state.settings.first { it.enabled } }.opacity,
            )

            updates.emit(PetSettings(enabled = false))
            assertFalse(withTimeout(5_000.milliseconds) { state.settings.first { !it.enabled } }.enabled)
        }
    }
}
