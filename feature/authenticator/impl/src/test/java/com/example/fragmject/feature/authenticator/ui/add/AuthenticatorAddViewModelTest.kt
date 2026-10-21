package com.example.fragmject.feature.authenticator.ui.add

import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.otp.OtpAlgorithm
import com.example.fragmject.core.domain.otp.OtpAuthUri
import com.example.fragmject.core.domain.otp.OtpMigrationUri
import com.example.fragmject.core.domain.otp.OtpType
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 新增账户页：校验先于落库、密钥归一化、扫码填表与批量导入的边界。
 */
class AuthenticatorAddViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank name and secret are rejected without hitting the repository`() = runTest {
        val repo = FakeOtpRepository()
        val viewModel = AuthenticatorAddViewModel(repo)

        viewModel.save()

        assertEquals("账户名不能为空", viewModel.uiState.value.nameError)
        assertEquals("密钥不能为空", viewModel.uiState.value.secretError)
        assertEquals(0, repo.added.size)
    }

    @Test
    fun `non base32 secret is rejected`() = runTest {
        val repo = FakeOtpRepository()
        val viewModel = AuthenticatorAddViewModel(repo)
        viewModel.updateAccountName("alice")
        viewModel.updateSecret("ABC1")

        viewModel.save()

        assertNotNull(viewModel.uiState.value.secretError)
        assertEquals(0, repo.added.size)
    }

    @Test
    fun `valid secret is normalized before persistence`() = runTest {
        val repo = FakeOtpRepository()
        val viewModel = AuthenticatorAddViewModel(repo)
        viewModel.updateIssuer("  ACME  ")
        viewModel.updateAccountName("  alice  ")
        viewModel.updateSecret("jbsw y3dp-ehpk 3pxp====")
        viewModel.updatePeriod("45")
        viewModel.updateAlgorithm(OtpAlgorithm.SHA256)

        val events = viewModel.events
        viewModel.save()

        val saved = repo.added.single()
        assertEquals("JBSWY3DPEHPK3PXP", saved.secret)
        assertEquals("alice", saved.accountName)
        assertEquals("ACME", saved.issuer)
        assertEquals(45, saved.period)
        assertEquals(OtpAlgorithm.SHA256, saved.algorithm)
        assertEquals(AuthenticatorAddEvent.Saved, events.first())
    }

    @Test
    fun `blank or zero period falls back to thirty seconds`() = runTest {
        val repo = FakeOtpRepository()
        val viewModel = AuthenticatorAddViewModel(repo)
        viewModel.updateAccountName("alice")
        viewModel.updateSecret("JBSWY3DPEHPK3PXP")

        viewModel.updatePeriod("abc")
        viewModel.save()
        assertEquals(30, repo.added.single().period)

        viewModel.updatePeriod("0")
        viewModel.save()
        assertEquals(30, repo.added.last().period)
    }

    @Test
    fun `period input keeps only four digits`() = runTest {
        val viewModel = AuthenticatorAddViewModel(FakeOtpRepository())

        viewModel.updatePeriod("12ab3456")

        assertEquals("1234", viewModel.uiState.value.periodText)
    }

    @Test
    fun `duplicate save is ignored while the first write is in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeOtpRepository(addGate = gate)
        val viewModel = AuthenticatorAddViewModel(repo)
        viewModel.updateAccountName("alice")
        viewModel.updateSecret("JBSWY3DPEHPK3PXP")

        viewModel.save()
        assertTrue(viewModel.uiState.value.saving)
        viewModel.save()
        assertEquals(1, repo.added.size)

        gate.complete(Unit)
        assertEquals(false, viewModel.uiState.value.saving)
    }

    @Test
    fun `scanning an otpauth uri fills the form without saving`() = runTest {
        val repo = FakeOtpRepository()
        val viewModel = AuthenticatorAddViewModel(repo)
        val raw = OtpAuthUri.generate(
            OtpAuthUri(
                type = OtpType.TOTP,
                secret = "JBSWY3DPEHPK3PXP",
                accountName = "alice@example.com",
                issuer = "ACME",
                digits = 8,
                period = 60,
            ),
        )

        val outcome = viewModel.applyScan(raw)

        assertEquals(ScanOutcome.Filled, outcome)
        assertEquals("alice@example.com", viewModel.uiState.value.accountName)
        assertEquals("ACME", viewModel.uiState.value.issuer)
        assertEquals("60", viewModel.uiState.value.periodText)
        assertEquals(8, viewModel.uiState.value.digits)
        assertTrue("扫码只填表，不应落库", repo.added.isEmpty())
    }

    @Test
    fun `single account migration uri still requires confirmation`() = runTest {
        val repo = FakeOtpRepository()
        val viewModel = AuthenticatorAddViewModel(repo)
        val raw = OtpMigrationUri.generate(listOf(account("a@example.com"))).first()

        val outcome = viewModel.applyScan(raw)

        assertEquals(ScanOutcome.Filled, outcome)
        assertEquals("a@example.com", viewModel.uiState.value.accountName)
        assertTrue(repo.added.isEmpty())
    }

    @Test
    fun `multi account migration uri is imported in one go`() = runTest {
        val repo = FakeOtpRepository()
        val viewModel = AuthenticatorAddViewModel(repo)
        val raw = OtpMigrationUri.generate(
            listOf(account("a@example.com"), account("b@example.com")),
        ).first()

        val outcome = viewModel.applyScan(raw)

        assertTrue(outcome is ScanOutcome.Imported)
        assertEquals(2, repo.added.size)
        assertEquals(AuthenticatorAddEvent.Imported(2), viewModel.events.first())
    }

    @Test
    fun `unrecognized content is reported instead of filling the form`() = runTest {
        val viewModel = AuthenticatorAddViewModel(FakeOtpRepository())

        assertEquals(ScanOutcome.Unrecognized, viewModel.applyScan("https://example.com/qr"))
        assertEquals("", viewModel.uiState.value.accountName)
        assertNull(viewModel.uiState.value.nameError)
    }

    private fun account(name: String) = OtpAccount(
        id = 0,
        issuer = "ACME",
        accountName = name,
        secret = "JBSWY3DPEHPK3PXP",
    )
}

private class FakeOtpRepository(
    private val addGate: CompletableDeferred<Unit>? = null,
) : OtpAccountRepository {
    private val accounts = MutableStateFlow<List<OtpAccount>>(emptyList())
    val added = mutableListOf<OtpAccount>()

    override fun observeAll() = accounts
    override suspend fun getById(id: Long): OtpAccount? = accounts.value.firstOrNull { it.id == id }

    override suspend fun add(account: OtpAccount): Long {
        added += account
        addGate?.await()
        return added.size.toLong()
    }

    override suspend fun update(account: OtpAccount) = Unit
    override suspend fun delete(id: Long) = Unit
}
