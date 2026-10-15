package com.example.fragmject.feature.authenticator.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.domain.otp.OtpAuthUri
import com.example.fragmject.core.domain.otp.OtpMigrationUri
import com.example.fragmject.core.domain.repository.OtpAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 二维码展示页状态。 */
data class ExportQrUiState(
    /** 待展示的二维码（可能多张）。 */
    val items: List<QrCodeItem> = emptyList(),
    /** 涉及的账户数量，用于页面副标题。 */
    val accountCount: Int = 0,
    /** 是否已完成入参设置（区分「还没传参」与「真的没有账户」）。 */
    val initialized: Boolean = false,
)

/**
 * 二维码展示页（导出流程第二步）：全屏展示二维码，不做任何选择交互。
 *
 * 入参由 NavKey 传入（[setSelection]），不依赖跨页面的隐式共享状态。
 */
@HiltViewModel
class AuthenticatorExportQrViewModel @Inject constructor(
    repository: OtpAccountRepository,
) : ViewModel() {

    private val ids = MutableStateFlow<List<Long>>(emptyList())
    private val batch = MutableStateFlow(false)
    private val initialized = MutableStateFlow(false)

    val uiState: StateFlow<ExportQrUiState> = combine(
        repository.observeAll(),
        ids,
        batch,
        initialized,
    ) { accounts, ids, batch, initialized ->
        val selected = accounts.filter { it.id in ids }
        ExportQrUiState(
            items = if (batch) selected.toBatchQrItems() else selected.map { it.toQrItem() },
            accountCount = selected.size,
            initialized = initialized,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportQrUiState())

    fun setSelection(ids: List<Long>, batch: Boolean) {
        this.ids.value = ids
        this.batch.value = batch
        initialized.value = true
    }

    private fun OtpAccount.toQrItem(): QrCodeItem = QrCodeItem(
        caption = displayIssuer,
        name = accountName,
        content = OtpAuthUri.generate(
            OtpAuthUri(
                type = type,
                secret = secret,
                accountName = accountName,
                issuer = issuer.takeIf { it.isNotBlank() },
                algorithm = algorithm,
                digits = digits,
                period = period,
                counter = counter,
            ),
        ),
    )

    /**
     * 批量编码：**可能返回多张码**（受二维码容量限制自动分批）。
     *
     * 分批数量对本页面就是「几张码」，caption 必须写明第几张，
     * 否则接收方会以为扫一张就导完了。
     */
    private fun List<OtpAccount>.toBatchQrItems(): List<QrCodeItem> {
        val uris = OtpMigrationUri.generate(this)
        return uris.mapIndexed { index, uri ->
            QrCodeItem(
                caption = if (uris.size == 1) {
                    "批量导出：${size} 个账户"
                } else {
                    "批量导出 ${index + 1}/${uris.size}（共 $size 个账户）"
                },
                name = if (uris.size == 1) {
                    "含 $size 个账户，可一次性导入"
                } else {
                    "请让对方把 ${uris.size} 张码依次扫完"
                },
                content = uri,
                size = QrCodeItem.BATCH_QR_SIZE,
            )
        }
    }
}
