package com.example.fragmject.feature.authenticator.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.otp.Base32
import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.otp.OtpAlgorithm
import com.example.fragmject.core.domain.otp.OtpAuthUri
import com.example.fragmject.core.domain.otp.OtpMigrationUri
import com.example.fragmject.core.domain.otp.OtpType
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 新增账户表单状态。字段即 UI 输入，错误为 null 表示通过校验。 */
data class AuthenticatorAddUiState(
    val issuer: String = "",
    val accountName: String = "",
    val secret: String = "",
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    /** 周期输入框的原始文本，保存时才解析为 [period]；允许中间态为空。 */
    val periodText: String = "30",
    val type: OtpType = OtpType.TOTP,
    val nameError: String? = null,
    val secretError: String? = null,
    val saving: Boolean = false,
)

sealed interface AuthenticatorAddEvent {
    /** 保存成功，UI 应返回列表页。 */
    data object Saved : AuthenticatorAddEvent

    /** 批量导入成功，UI 提示后返回列表页。 */
    data class Imported(val count: Int) : AuthenticatorAddEvent
}

/**
 * 扫码结果：交给 UI 决定「显示成功 / 显示失败 / 切到表单」。
 *
 * 拆成三种而不是一个 Boolean：批量导入与单账户填表后续动作完全不同
 * （前者直接落库返回，后者要切到表单让用户确认），用一个布尔值表达不了。
 */
sealed interface ScanOutcome {
    /** 已填入表单，等用户确认后保存。 */
    data object Filled : ScanOutcome

    /** 已直接批量导入。 */
    data class Imported(val count: Int) : ScanOutcome

    /** 内容不是可识别的账户二维码。 */
    data object Unrecognized : ScanOutcome
}

/**
 * 新增账户页：扫码导入与手输密钥共用同一份表单状态。
 *
 * 扫码只负责**填表**，不直接落库——让用户确认后再保存，避免扫到别人的
 * 二维码就静默写入一个账户。
 */
@HiltViewModel
class AuthenticatorAddViewModel @Inject constructor(
    private val repository: OtpAccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthenticatorAddUiState())
    val uiState: StateFlow<AuthenticatorAddUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthenticatorAddEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun updateIssuer(value: String) = _uiState.update { it.copy(issuer = value) }

    fun updateAccountName(value: String) =
        _uiState.update { it.copy(accountName = value, nameError = null) }

    fun updateSecret(value: String) = _uiState.update { it.copy(secret = value, secretError = null) }

    fun updateAlgorithm(value: OtpAlgorithm) = _uiState.update { it.copy(algorithm = value) }

    fun updateDigits(value: Int) = _uiState.update { it.copy(digits = value) }

    /** 周期输入保持原始文本，只过滤非数字（限 4 位），避免删字时被强制回填 30。 */
    fun updatePeriod(value: String) {
        _uiState.update { it.copy(periodText = value.filter { c -> c.isDigit() }.take(4)) }
    }

    /**
     * 用扫码结果填充表单或直接批量导入。
     *
     * - `otpauth://`（单账户）：只填表，用户确认后再保存；
     * - `otpauth-migration://`（Google Authenticator 批量导出）：
     *   单账户仍走填表确认；多账户按用户明确的「导出/导入」意图直接全部落库。
     *
     * @return 见 [ScanOutcome]。
     */
    fun applyScan(raw: String): ScanOutcome {
        if (raw.startsWith("otpauth-migration://", ignoreCase = true)) {
            val accounts = OtpMigrationUri.parse(raw)
            if (accounts.isEmpty()) return ScanOutcome.Unrecognized
            if (accounts.size == 1) {
                fillForm(accounts.first())
                return ScanOutcome.Filled
            }
            viewModelScope.launch {
                accounts.forEach { repository.add(it) }
                _events.send(AuthenticatorAddEvent.Imported(accounts.size))
            }
            return ScanOutcome.Imported(accounts.size)
        }
        val uri = OtpAuthUri.parse(raw) ?: return ScanOutcome.Unrecognized
        fillForm(
            OtpAccount(
                issuer = uri.issuer.orEmpty(),
                accountName = uri.accountName,
                secret = uri.secret,
                algorithm = uri.algorithm,
                digits = uri.digits,
                period = uri.period,
                type = uri.type,
                counter = uri.counter,
            ),
        )
        return ScanOutcome.Filled
    }

    private fun fillForm(account: OtpAccount) {
        _uiState.update {
            it.copy(
                issuer = account.issuer,
                accountName = account.accountName,
                secret = account.secret,
                algorithm = account.algorithm,
                digits = account.digits,
                periodText = account.period.toString(),
                type = account.type,
                nameError = null,
                secretError = null,
            )
        }
    }

    /** 校验并保存；失败时把错误写回状态，成功时发出 [AuthenticatorAddEvent.Saved]。 */
    fun save() {
        val state = _uiState.value
        if (state.saving) return

        val accountName = state.accountName.trim()
        val secret = normalizeSecret(state.secret)
        val period = state.periodText.toIntOrNull()?.takeIf { it > 0 } ?: 30
        val nameError = if (accountName.isBlank()) "账户名不能为空" else null
        val secretError = when {
            secret.isEmpty() -> "密钥不能为空"
            !Base32.isValid(secret) -> "密钥不是合法的 Base32（仅 A-Z、2-7）"
            else -> null
        }
        if (nameError != null || secretError != null) {
            _uiState.update { it.copy(nameError = nameError, secretError = secretError) }
            return
        }

        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.add(
                OtpAccount(
                    issuer = state.issuer.trim(),
                    accountName = accountName,
                    secret = secret,
                    algorithm = state.algorithm,
                    digits = state.digits,
                    period = period,
                    type = state.type,
                ),
            )
            _events.send(AuthenticatorAddEvent.Saved)
            _uiState.update { it.copy(saving = false) }
        }
    }

    /**
     * 归一化手输密钥：去空格与连字符、转大写、去尾部填充。
     *
     * 用户从网页复制的密钥常带空格分组，不归一化会直接判定为非法，
     * 或者更糟——存进去一个解不出来的密钥。
     */
    private fun normalizeSecret(value: String): String =
        value.replace(" ", "").replace("-", "").uppercase().trimEnd('=')
}
