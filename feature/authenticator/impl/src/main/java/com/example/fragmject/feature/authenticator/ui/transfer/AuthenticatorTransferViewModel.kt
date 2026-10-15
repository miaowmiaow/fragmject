package com.example.fragmject.feature.authenticator.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import com.example.fragmject.core.navigation.contract.AuthenticatorExportMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** 选择账户页状态：只管「选谁、用什么格式导出」，二维码渲染在独立的展示页。
 *
 * 导出格式复用契约层的 [AuthenticatorExportMode]：它同时是导航动作的参数类型，
 * impl 侧再定义一份同名枚举会让「UI 选中态」与「导航参数」出现两套真相。
 */
data class AuthenticatorTransferUiState(
    val accounts: List<OtpAccount> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val mode: AuthenticatorExportMode = AuthenticatorExportMode.SINGLE,
) {
    /** 已选数量；决定「生成二维码」按钮是否可点。 */
    val selectedCount: Int get() = selectedIds.size
}

/**
 * 选择账户页（导出流程第一步）。
 *
 * 只做选择与格式切换，选完点「生成二维码」跳到展示页——
 * 二维码是需要被另一台设备**完整拍下**的东西，不能和勾选列表挤在同一屏。
 */
@HiltViewModel
class AuthenticatorTransferViewModel @Inject constructor(
    repository: OtpAccountRepository,
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val mode = MutableStateFlow(AuthenticatorExportMode.SINGLE)

    val uiState: StateFlow<AuthenticatorTransferUiState> = combine(
        repository.observeAll(),
        selectedIds,
        mode,
    ) { accounts, ids, exportMode ->
        // 不默认全选：导出常是「挑几个迁走」，默认全选容易把不想迁的也导出去
        AuthenticatorTransferUiState(
            accounts = accounts,
            selectedIds = ids,
            mode = exportMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthenticatorTransferUiState())

    fun toggle(id: Long) = selectedIds.update {
        val current = uiState.value.selectedIds
        if (id in current) current - id else current + id
    }

    /** 全选 / 取消全选。 */
    fun selectAll() = selectedIds.update {
        val current = uiState.value.selectedIds
        val all = uiState.value.accounts.map { it.id }.toSet()
        if (current.containsAll(all)) emptySet() else all
    }

    fun setMode(value: AuthenticatorExportMode) = mode.update { value }
}
