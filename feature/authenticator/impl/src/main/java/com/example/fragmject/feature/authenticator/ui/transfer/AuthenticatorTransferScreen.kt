package com.example.fragmject.feature.authenticator.ui.transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.domain.otp.OtpAccount
import com.example.fragmject.core.navigation.contract.AuthenticatorExportMode
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.feature.authenticator.components.AnimatedTabBar
import com.example.fragmject.feature.authenticator.components.AuthenticatorAppBar

/**
 * 选择账户页（导出流程第一步）：勾选要导出的账户 + 选导出方式，然后跳展示页。
 *
 * 本页**不再渲染二维码**（二维码展示在独立的 [AuthenticatorExportQrScreen]）：
 * 二维码必须能被另一台设备完整拍下，和勾选列表同屏时只能压成半屏、被裁掉。
 */
@Composable
fun AuthenticatorTransferScreen(
    viewModel: AuthenticatorTransferViewModel = hiltViewModel(),
) {
    // 跟随 app 当前的亮/暗方案（见 AuthenticatorScreen 同款注释）
    AuthenticatorTransferScreenContent(viewModel = viewModel)
}

@Composable
private fun AuthenticatorTransferScreenContent(
    viewModel: AuthenticatorTransferViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onNavigateUp = LocalOnNavigateUp.current
    val navigators = LocalNavigators.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AuthenticatorAppBar(title = "选择要导出的账户", onBackClick = onNavigateUp) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.accounts.isEmpty()) {
                TransferEmptyHint()
                return@Scaffold
            }
            TransferSelectionHeader(
                selectedCount = uiState.selectedIds.size,
                totalCount = uiState.accounts.size,
                allSelected = uiState.selectedIds.containsAll(uiState.accounts.map { it.id }),
                onToggleAll = viewModel::selectAll,
            )
            ExportModeSelector(mode = uiState.mode, onModeChange = viewModel::setMode)
            TransferAccountList(
                accounts = uiState.accounts,
                selectedIds = uiState.selectedIds,
                onToggle = viewModel::toggle,
            )
            TransferSubmitButton(
                enabled = uiState.selectedIds.isNotEmpty(),
                selectedCount = uiState.selectedIds.size,
                onClick = {
                    // 库内顺序导出：勾选顺序不影响结果
                    val ids = uiState.accounts
                        .filter { it.id in uiState.selectedIds }
                        .map { it.id }
                    navigators.authenticator.openAuthenticatorExportQr(
                        ids = ids,
                        mode = uiState.mode,
                    )
                },
            )
        }
    }
}

/** 无账户时的占位提示。 */
@Composable
private fun TransferEmptyHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "还没有可导出的账户",
            fontSize = 14.sp,
        )
    }
}

/** 已选计数 + 全选/取消全选。 */
@Composable
private fun TransferSelectionHeader(
    selectedCount: Int,
    totalCount: Int,
    allSelected: Boolean,
    onToggleAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "已选 $selectedCount / $totalCount 个账户",
            fontSize = 13.sp,
        )
        Text(
            text = if (allSelected) "取消全选" else "全选",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .clickable(onClick = onToggleAll)
                .padding(5.dp),
        )
    }
}

/** 单账户码 / 批量码切换 + 对应说明。 */
@Composable
private fun ExportModeSelector(
    mode: AuthenticatorExportMode,
    onModeChange: (AuthenticatorExportMode) -> Unit,
) {
    AnimatedTabBar(
        selectedTab = if (mode == AuthenticatorExportMode.SINGLE) 0 else 1,
        onTabSelected = {
            onModeChange(
                if (it == 0) AuthenticatorExportMode.SINGLE else AuthenticatorExportMode.BATCH,
            )
        },
        tabs = listOf("单账户码", "批量码"),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    Text(
        text = if (mode == AuthenticatorExportMode.SINGLE) {
            "每个账户一张 otpauth:// 二维码，任何验证器都能扫"
        } else {
            "一张码装多个账户（otpauth-migration://），账户多时分成多张"
        },
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/** 可勾选的账户列表；`weight` 需要 ColumnScope 接收者。 */
@Composable
private fun ColumnScope.TransferAccountList(
    accounts: List<OtpAccount>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        items(accounts, key = { it.id }) { account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(account.id) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = account.id in selectedIds,
                    onCheckedChange = { onToggle(account.id) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        uncheckedColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = account.displayIssuer, fontSize = 13.sp)
                    if (account.accountName.isNotBlank()) {
                        Text(text = account.accountName, fontSize = 11.sp)
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

/** 底部「生成二维码」按钮。 */
@Composable
private fun TransferSubmitButton(
    enabled: Boolean,
    selectedCount: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(
            text = if (selectedCount == 0) "请先勾选账户" else "生成 $selectedCount 个账户的二维码",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
