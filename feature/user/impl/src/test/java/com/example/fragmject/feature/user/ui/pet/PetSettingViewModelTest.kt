package com.example.fragmject.feature.user.ui.pet

import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.domain.repository.PetSettingsRepository
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * [PetSettingViewModel] 测试：读侧跟随仓库、写侧逐字段转发。
 *
 * 写侧刻意断言「每个 setter 只改自己那个字段」——这是防止两个滑杆互相覆盖的契约，
 * 实现若被改成 `save(settings)` 整份覆盖，这里会失败。
 */
class PetSettingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val epsilon = 0.0001f

    @Test
    fun `无订阅时保持默认值，订阅后跟随仓库`() = runTest {
        val repository = FakePetSettingsRepository()
        val viewModel = PetSettingViewModel(repository)

        // WhileSubscribed：没有人收流时不应启动上游，值停在默认
        assertEquals(PetSettings.Default, viewModel.settings.value)

        backgroundScope.launch { viewModel.settings.collect { } }
        // 订阅协程跑在 runTest 的 StandardTestDispatcher 上，必须显式推进一步才会上游生效
        runCurrent()
        repository.emit(PetSettings(opacity = 0.5f, sizeScale = 1.5f))
        runCurrent()
        assertEquals(0.5f, viewModel.settings.value.opacity, epsilon)
        assertEquals(1.5f, viewModel.settings.value.sizeScale, epsilon)
    }

    @Test
    fun `每个 setter 只写自己那个字段`() = runTest {
        val repository = FakePetSettingsRepository()
        val viewModel = PetSettingViewModel(repository)
        backgroundScope.launch { viewModel.settings.collect { } }

        viewModel.setOpacity(0.6f)
        assertEquals(0.6f, repository.state.value.opacity, epsilon)
        assertEquals(PetSettings.Default.sizeScale, repository.state.value.sizeScale, epsilon)

        viewModel.setSizeScale(1.3f)
        assertEquals(1.3f, repository.state.value.sizeScale, epsilon)
        assertEquals(0.6f, repository.state.value.opacity, epsilon)

        viewModel.setSpeedScale(1.7f)
        assertEquals(1.7f, repository.state.value.speedScale, epsilon)

        viewModel.setEnabled(false)
        assertFalse(repository.state.value.enabled)
    }

    private class FakePetSettingsRepository : PetSettingsRepository {
        val state = MutableStateFlow(PetSettings.Default)

        fun emit(settings: PetSettings) {
            state.value = settings
        }

        override fun observePetSettings(): Flow<PetSettings> = state

        override suspend fun setEnabled(enabled: Boolean) {
            state.value = state.value.copy(enabled = enabled)
        }

        override suspend fun setOpacity(opacity: Float) {
            state.value = state.value.copy(opacity = opacity)
        }

        override suspend fun setSizeScale(scale: Float) {
            state.value = state.value.copy(sizeScale = scale)
        }

        override suspend fun setSpeedScale(scale: Float) {
            state.value = state.value.copy(speedScale = scale)
        }
    }
}
