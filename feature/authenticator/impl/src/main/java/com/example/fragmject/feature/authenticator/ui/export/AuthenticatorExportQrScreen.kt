package com.example.fragmject.feature.authenticator.ui.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.feature.authenticator.components.AuthenticatorAppBar

/**
 * 二维码展示页（导出流程第二步）：**全屏**展示二维码，不做选择交互。
 *
 * 与参考项目 `ExportScreen` 一致——二维码是要被另一台设备完整拍下的东西，
 * 页面里不能有任何与它抢空间的元素。多张码时 WebView 内部上下滑动。
 */
@Composable
fun AuthenticatorExportQrScreen(
    ids: List<Long>,
    batch: Boolean,
    viewModel: AuthenticatorExportQrViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onNavigateUp = LocalOnNavigateUp.current

    LaunchedEffect(ids, batch) {
        viewModel.setSelection(ids = ids, batch = batch)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AuthenticatorAppBar(title = "导出二维码", onBackClick = onNavigateUp) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                text = when {
                    !uiState.initialized -> "正在生成二维码…"
                    uiState.items.isEmpty() -> "没有可导出的账户"
                    uiState.items.size == 1 -> "共 ${uiState.accountCount} 个账户"
                    else -> "共 ${uiState.accountCount} 个账户 · ${uiState.items.size} 张码，上下滑动查看"
                },
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.initialized) {
                        Text(
                            text = "没有可导出的账户",
                            fontSize = 14.sp,
                        )
                    } else {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            trackColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                QrCodeScreen(
                    items = uiState.items,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}
