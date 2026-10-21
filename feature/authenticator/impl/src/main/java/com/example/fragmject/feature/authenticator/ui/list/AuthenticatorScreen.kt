package com.example.fragmject.feature.authenticator.ui.list

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.feature.authenticator.components.AccountCard
import com.example.fragmject.feature.authenticator.components.AnimatedFab
import com.example.fragmject.feature.authenticator.components.AppBarTextAction
import com.example.fragmject.feature.authenticator.components.AuthenticatorAppBar
import com.example.fragmject.feature.authenticator.components.EmptyState
import com.example.fragmject.feature.authenticator.model.OtpCodeItem
import kotlinx.coroutines.launch

/** 弹窗动作集合：删除确认、详情里的复制/上移/下移与关闭。 */
@Stable
private class AuthenticatorDialogActions(
    val onDelete: (Long) -> Unit,
    val onMoveUp: (Long) -> Unit,
    val onMoveDown: (Long) -> Unit,
    val onCopy: (String) -> Unit,
    val onDismissDelete: () -> Unit,
    val onDismissDetail: () -> Unit,
)

@Composable
fun AuthenticatorScreen(
    viewModel: AuthenticatorViewModel = hiltViewModel(),
) {
    // 跟随 app 当前的亮/暗方案（根节点 AppTheme 已注入），不再强制暗色：
    // 顶栏 #272A36 与强调色 #FF8B80 在两套方案中取值相同，不受影响
    AuthenticatorScreenContent(viewModel = viewModel)
}

@Composable
private fun AuthenticatorScreenContent(
    viewModel: AuthenticatorViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigators = LocalNavigators.current
    val onNavigateUp = LocalOnNavigateUp.current
    val onTransferClick =
        remember(navigators) { { navigators.authenticator.openAuthenticatorTransfer() } }
    val onAddClick = remember(navigators) { { navigators.authenticator.openAuthenticatorAdd() } }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var detailItem by remember { mutableStateOf<OtpCodeItem?>(null) }
    val items = uiState.items
    val copyToClipboard: (String, String) -> Unit = { text, message ->
        scope.launch {
            clipboard.setClipEntry(ClipData.newPlainText(text, text).toClipEntry())
            snackbarHostState.showSnackbar(message)
        }
    }
    AuthenticatorDialogs(
        pendingDeleteId = pendingDeleteId,
        detailItem = detailItem,
        items = items,
        actions = AuthenticatorDialogActions(
            onDelete = { id -> viewModel.delete(id); pendingDeleteId = null },
            onMoveUp = { id -> viewModel.moveUp(id); detailItem = null },
            onMoveDown = { id -> viewModel.moveDown(id); detailItem = null },
            onCopy = { text -> copyToClipboard(text, "已复制") },
            onDismissDelete = { pendingDeleteId = null },
            onDismissDetail = { detailItem = null },
        ),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // 顶栏自绘背景并自己处理状态栏 inset（与 core:designsystem 的 TitleBar 同一做法），
        // 因此放入 topBar 槽位，内容的 innerPadding 不再包含状态栏，避免双重留白
        topBar = {
            AuthenticatorAppBar(
                title = "身份验证器",
                onBackClick = onNavigateUp,
                actions = {
                    AppBarTextAction(text = "导出", onClick = onTransferClick)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedFab(onClick = onAddClick, modifier = Modifier.padding(bottom = 20.dp))
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.timeDriftMillis != 0L) {
                TimeDriftWarning(
                    driftMillis = uiState.timeDriftMillis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            if (!uiState.isLoading && items.isEmpty()) {
                EmptyState(
                    title = "还没有账户",
                    description = "点击右下角 + 扫码或手输密钥添加",
                    modifier = Modifier.weight(1f),
                )
            } else {
                AuthenticatorAccountList(
                    modifier = Modifier.weight(1f),
                    items = items,
                    onCopy = { item -> copyToClipboard(item.code, "已复制验证码") },
                    onLongClick = { detailItem = it },
                    onDeleteRequest = { pendingDeleteId = it },
                )
            }
        }
    }
}

/** 账户列表：左滑露出删除背景，松手即请求确认。 */
@Composable
private fun AuthenticatorAccountList(
    items: List<OtpCodeItem>,
    onCopy: (OtpCodeItem) -> Unit,
    onLongClick: (OtpCodeItem) -> Unit,
    onDeleteRequest: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.account.id }) { index, item ->
            val dismissState = rememberSwipeToDismissBoxState()
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onDeleteRequest(item.account.id)
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 2.dp)
                            .alpha(0.85f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 24.dp),
                        )
                    }
                },
            ) {
                AccountCard(
                    item = item,
                    onCopy = { onCopy(item) },
                    onLongClick = { onLongClick(item) },
                )
            }
            if (index == items.lastIndex) {
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

/** 删除确认与账户详情两个弹窗，按当前状态按需展示。 */
@Composable
private fun AuthenticatorDialogs(
    pendingDeleteId: Long?,
    detailItem: OtpCodeItem?,
    items: List<OtpCodeItem>,
    actions: AuthenticatorDialogActions,
) {
    pendingDeleteId?.let { id ->
        items.firstOrNull { it.account.id == id }?.let { target ->
            DeleteConfirmDialog(
                issuer = target.account.displayIssuer,
                onConfirm = { actions.onDelete(id) },
                onDismiss = actions.onDismissDelete,
            )
        }
    }
    detailItem?.let { item ->
        AccountDetailDialog(
            item = item,
            canMoveUp = items.firstOrNull()?.account?.id != item.account.id,
            canMoveDown = items.lastOrNull()?.account?.id != item.account.id,
            onCopy = actions.onCopy,
            onMoveUp = { actions.onMoveUp(item.account.id) },
            onMoveDown = { actions.onMoveDown(item.account.id) },
            onDismiss = actions.onDismissDetail,
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    issuer: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除账户") },
        text = { Text("确定删除「$issuer」吗？删除后需要重新扫码或手输密钥才能恢复。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

@Composable
private fun AccountDetailDialog(
    item: OtpCodeItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCopy: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.account.displayIssuer) },
        text = {
            Column {
                DetailRow("账户名", item.account.accountName, onCopy)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DetailRow("密钥（Base32）", item.account.secret, onCopy)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                val meta = "${item.account.algorithm.wireValue} · ${item.account.digits} 位 · ${item.account.period}s"
                DetailRow("算法 / 位数 / 周期", meta, onCopy)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    ) { Text("上移") }
                    TextButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    ) { Text("下移") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
            ) { Text("关闭") }
        },
        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onCopy(value) }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制$label",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** 设备时间与标准时间偏差过大时的提示条（TOTP 依赖时间，偏差会导致验证码错误）。 */
@Composable
private fun TimeDriftWarning(
    driftMillis: Long,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 项目暗色方案的 error / errorContainer / onErrorContainer 都是纯红，
    // 直接套用会变成「红底红字」，因此文字与图标显式用白色
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
                )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "设备时间可能不准",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "与标准时间相差约 ${kotlin.math.abs(driftMillis) / 1000} 秒，可能导致验证码错误",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
            Button(
                onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_DATE_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("去校准", fontSize = 12.sp)
            }
        }
    }
}
