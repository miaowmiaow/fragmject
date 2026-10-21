package com.example.fragmject.feature.authenticator.ui.export

import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 二维码展示页：入参决定选中账户，单张 / 批量两种编码路径不能混淆。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatorExportQrViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - selection is not applied yet`() = runTest {
        val viewModel = AuthenticatorExportQrViewModel(FakeOtpRepository(listOf(account(1))))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertFalse(viewModel.uiState.value.initialized)
        assertTrue(viewModel.uiState.value.items.isEmpty())

        job.cancel()
    }

    @Test
    fun `single account selection produces one otpauth qr`() = runTest {
        val viewModel = AuthenticatorExportQrViewModel(FakeOtpRepository(listOf(account(1), account(2))))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.setSelection(listOf(1), batch = false)

        val state = viewModel.uiState.value
        assertTrue(state.initialized)
        assertEquals(1, state.accountCount)
        assertEquals(1, state.items.size)
        assertTrue(state.items.single().content.startsWith("otpauth://"))

        job.cancel()
    }

    @Test
    fun `unknown ids are ignored`() = runTest {
        val viewModel = AuthenticatorExportQrViewModel(FakeOtpRepository(listOf(account(1))))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.setSelection(listOf(99), batch = false)

        assertEquals(0, viewModel.uiState.value.accountCount)
        assertTrue(viewModel.uiState.value.items.isEmpty())

        job.cancel()
    }

    @Test
    fun `batch selection produces migration qr with batch caption`() = runTest {
        val viewModel = AuthenticatorExportQrViewModel(
            FakeOtpRepository(listOf(account(1), account(2), account(3))),
        )
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.setSelection(listOf(1, 2, 3), batch = true)

        val state = viewModel.uiState.value
        assertEquals(3, state.accountCount)
        assertTrue(state.items.isNotEmpty())
        assertTrue(state.items.first().caption.startsWith("批量导出"))
        assertTrue(state.items.first().content.startsWith("otpauth-migration://"))

        job.cancel()
    }

    private fun account(id: Long) = OtpAccount(
        id = id,
        issuer = "ACME",
        accountName = "user$id",
        secret = "JBSWY3DPEHPK3PXP",
    )
}

private class FakeOtpRepository(initial: List<OtpAccount>) : OtpAccountRepository {
    private val accounts = MutableStateFlow(initial)

    override fun observeAll() = accounts
    override suspend fun getById(id: Long): OtpAccount? = accounts.value.firstOrNull { it.id == id }
    override suspend fun add(account: OtpAccount): Long = 0L
    override suspend fun update(account: OtpAccount) = Unit
    override suspend fun delete(id: Long) = Unit
}
