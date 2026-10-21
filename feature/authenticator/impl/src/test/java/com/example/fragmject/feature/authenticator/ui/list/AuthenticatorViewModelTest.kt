package com.example.fragmject.feature.authenticator.ui.list

import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import com.example.fragmject.core.testing.MainDispatcherRule
import com.example.fragmject.feature.authenticator.utils.Clock
import com.example.fragmject.feature.authenticator.utils.DRIFT_THRESHOLD_MS
import com.example.fragmject.feature.authenticator.utils.TimeOffsetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 验证器列表页：时间与漂移全部来自注入端口，因此验证码结果可复现。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `emits a code per account using the injected clock`() = runTest {
        val viewModel = createViewModel()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertTrue(state is AuthenticatorUiState.Success)
        val item = (state as AuthenticatorUiState.Success).items.single()
        assertEquals(ACCOUNT.id, item.account.id)
        assertEquals(6, item.code.length)
        assertEquals(ACCOUNT.period * 1000L, item.periodMillis)
        assertTrue(item.isTimeBased)

        job.cancel()
    }

    @Test
    fun `drift above the threshold is surfaced`() = runTest {
        val drift = DRIFT_THRESHOLD_MS + 15_000L
        val viewModel = createViewModel(offset = FakeOffset(drift))

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(drift, viewModel.uiState.value.timeDriftMillis)
        job.cancel()
    }

    @Test
    fun `small drift is hidden from the user`() = runTest {
        val viewModel = createViewModel(offset = FakeOffset(DRIFT_THRESHOLD_MS - 1))

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(0L, viewModel.uiState.value.timeDriftMillis)
        job.cancel()
    }

    @Test
    fun `sntp failure degrades to no drift instead of an error state`() = runTest {
        val viewModel = createViewModel(offset = FakeOffset(null))

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertTrue(viewModel.uiState.value is AuthenticatorUiState.Success)
        assertEquals(0L, viewModel.uiState.value.timeDriftMillis)
        job.cancel()
    }

    @Test
    fun `ticker stops after the screen stops collecting`() = runTest {
        val clock = FakeClock(NOW)
        val viewModel = createViewModel(clock = clock)
        // ticker 跑在 viewModelScope（Main）上，必须推进 Main dispatcher 自带的时间轴，
        // 而不是 runTest 自己的 scheduler。
        val scheduler = mainDispatcherRule.testDispatcher.scheduler

        val job = launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect() }
        scheduler.advanceTimeBy(1_200)
        scheduler.runCurrent()
        val callsWhileVisible = clock.calls
        assertTrue("ticker should keep advancing while visible, calls=$callsWhileVisible", callsWhileVisible > 1)

        job.cancel()
        // 两层 WhileSubscribed(5s)：uiState 先停 combine，nowMillis 再停 ticker，最坏 10s
        // 才完全静默。留 1s 余量后再取快照，之后推进时间不应再读时钟。
        scheduler.advanceTimeBy(11_000)
        scheduler.runCurrent()
        val callsAfterStop = clock.calls

        scheduler.advanceTimeBy(10_000)
        scheduler.runCurrent()
        assertEquals(callsAfterStop, clock.calls)
    }

    private fun createViewModel(
        clock: Clock = FakeClock(NOW),
        offset: TimeOffsetProvider = FakeOffset(null),
    ): AuthenticatorViewModel = AuthenticatorViewModel(
        repository = FakeOtpAccountRepository(listOf(ACCOUNT)),
        clock = clock,
        timeOffsetProvider = offset,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L

        val ACCOUNT = OtpAccount(
            id = 1,
            issuer = "ACME",
            accountName = "alice@example.com",
            secret = "JBSWY3DPEHPK3PXP",
        )
    }
}

private class FakeOtpAccountRepository(initial: List<OtpAccount>) : OtpAccountRepository {
    private val accounts = MutableStateFlow(initial)

    override fun observeAll() = accounts
    override suspend fun getById(id: Long): OtpAccount? = accounts.value.firstOrNull { it.id == id }
    override suspend fun add(account: OtpAccount): Long = 0L
    override suspend fun update(account: OtpAccount) = Unit
    override suspend fun delete(id: Long) = Unit
}

private class FakeClock(var now: Long) : Clock {
    var calls = 0
        private set

    override fun nowMillis(): Long {
        calls++
        return now
    }
}

private class FakeOffset(private val offset: Long?) : TimeOffsetProvider {
    override suspend fun offsetMillis(): Long? = offset
}
