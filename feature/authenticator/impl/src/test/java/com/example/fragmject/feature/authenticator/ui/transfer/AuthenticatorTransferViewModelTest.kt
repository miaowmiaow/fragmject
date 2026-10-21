package com.example.fragmject.feature.authenticator.ui.transfer

import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import com.example.fragmject.core.navigation.contract.AuthenticatorExportMode
import com.example.fragmject.core.testing.MainDispatcherRule
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
 * 导出选择页：默认不选中任何账户，全选可反选，格式切换独立于选择结果。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatorTransferViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - accounts flow is published without preselecting`() = runTest {
        val repo = FakeOtpRepository(listOf(account(1), account(2)))
        val viewModel = AuthenticatorTransferViewModel(repo)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(2, viewModel.uiState.value.accounts.size)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
        assertEquals(AuthenticatorExportMode.SINGLE, viewModel.uiState.value.mode)

        job.cancel()
    }

    @Test
    fun `toggle adds and removes the same account`() = runTest {
        val viewModel = AuthenticatorTransferViewModel(FakeOtpRepository(listOf(account(1), account(2))))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.toggle(1)
        assertEquals(setOf(1L), viewModel.uiState.value.selectedIds)
        assertEquals(1, viewModel.uiState.value.selectedCount)

        viewModel.toggle(1)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())

        job.cancel()
    }

    @Test
    fun `select all toggles between all and none`() = runTest {
        val viewModel = AuthenticatorTransferViewModel(FakeOtpRepository(listOf(account(1), account(2))))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.selectAll()
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.selectedIds)

        viewModel.selectAll()
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())

        job.cancel()
    }

    @Test
    fun `mode switch is independent from selection`() = runTest {
        val viewModel = AuthenticatorTransferViewModel(FakeOtpRepository(listOf(account(1))))
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.toggle(1)
        viewModel.setMode(AuthenticatorExportMode.BATCH)

        assertEquals(AuthenticatorExportMode.BATCH, viewModel.uiState.value.mode)
        assertEquals(setOf(1L), viewModel.uiState.value.selectedIds)

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
