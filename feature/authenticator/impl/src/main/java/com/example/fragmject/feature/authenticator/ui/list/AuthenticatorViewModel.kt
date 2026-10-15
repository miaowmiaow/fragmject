package com.example.fragmject.feature.authenticator.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.otp.Base32
import com.example.fragmject.core.domain.otp.Hotp
import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.otp.OtpType
import com.example.fragmject.core.domain.otp.Totp
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import com.example.fragmject.feature.authenticator.model.OtpCodeItem
import com.example.fragmject.feature.authenticator.utils.SntpTimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** 时间推进间隔：既要让倒计时环平滑，又不必每帧重算 HMAC。 */
private const val TICK_MILLIS = 500L

/** 密钥非法（手输时存进了非 Base32）时的占位码，避免 UI 崩溃或显示空串。 */
private const val INVALID_SECRET_CODE = "------"

sealed interface AuthenticatorUiState {
    data object Loading : AuthenticatorUiState
    data class Success(
        val items: List<OtpCodeItem> = emptyList(),
        /** 设备时间与标准时间的偏移毫秒；null = 未检测出明显漂移。 */
        val timeDriftMillis: Long? = null,
    ) : AuthenticatorUiState
}

/** Screen 用：避免每处都写 `as? Success`。 */
val AuthenticatorUiState.items: List<OtpCodeItem>
    get() = (this as? AuthenticatorUiState.Success)?.items ?: emptyList()

val AuthenticatorUiState.isLoading: Boolean
    get() = this is AuthenticatorUiState.Loading

val AuthenticatorUiState.timeDriftMillis: Long
    get() = (this as? AuthenticatorUiState.Success)?.timeDriftMillis ?: 0L

/**
 * 账户列表页。
 *
 * 验证码是「密钥 + 时间」的确定性结果，不需要后端，因此这里直接组合
 * 「账户流 × 时间流」即得：账户变化或时间跨过周期边界都会自动刷新。
 */
@HiltViewModel
class AuthenticatorViewModel @Inject constructor(
    private val repository: OtpAccountRepository,
) : ViewModel() {

    /** 持续推进的当前时间，驱动验证码刷新与倒计时。 */
    private val nowMillis: StateFlow<Long> = tickerFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    /** 时间漂移（毫秒）；只查一次，UDP 不通或漂移不大时为 null。 */
    private val timeDrift = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AuthenticatorUiState> = combine(
        repository.observeAll(),
        nowMillis,
        timeDrift,
    ) { accounts, now, drift ->
        AuthenticatorUiState.Success(
            items = accounts.map { it.toItem(now) },
            timeDriftMillis = drift?.take { kotlin.math.abs(it) > SntpTimeSource.DRIFT_THRESHOLD_MS },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthenticatorUiState.Loading)

    init {
        checkTimeDrift()
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** 上移一位：与相邻项交换 sort_order 后各自更新。 */
    fun moveUp(id: Long) = swapWithNeighbor(id, offset = -1)

    /** 下移一位。 */
    fun moveDown(id: Long) = swapWithNeighbor(id, offset = 1)

    private fun checkTimeDrift() {
        viewModelScope.launch {
            timeDrift.value = SntpTimeSource.queryOffsetMillis()
        }
    }

    private fun swapWithNeighbor(id: Long, offset: Int) {
        val items = uiState.value.items
        val index = items.indexOfFirst { it.account.id == id }
        val target = index + offset
        if (index < 0 || target !in items.indices) return
        val self = items[index].account
        val neighbor = items[target].account
        viewModelScope.launch {
            repository.update(self.copy(sortOrder = neighbor.sortOrder))
            repository.update(neighbor.copy(sortOrder = self.sortOrder))
        }
    }

    private fun OtpAccount.toItem(now: Long): OtpCodeItem = OtpCodeItem(
        account = this,
        code = codeAt(now),
        remainingMillis = if (type == OtpType.TOTP) Totp.remainingMillis(now, period) else 0L,
        periodMillis = period * 1000L,
    )

    private fun OtpAccount.codeAt(now: Long): String {
        val secretBytes = Base32.decode(secret) ?: return INVALID_SECRET_CODE
        return if (type == OtpType.TOTP) {
            Totp.generate(secretBytes, now, period, digits, algorithm)
        } else {
            Hotp.generate(secretBytes, counter, algorithm, digits)
        }
    }

    private fun tickerFlow(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MILLIS.milliseconds)
        }
    }
}

/** 过滤辅助：给 `take` 语义化（漂移不超过阈值时丢弃）。 */
private fun <T> T?.take(predicate: (T) -> Boolean): T? = this?.takeIf(predicate)
