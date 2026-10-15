package com.example.fragmject.feature.demo.ui.barcodescanning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.qrscanner.QrCodeScannerContainer
import kotlinx.coroutines.launch

/**
 * 二维码扫描演示页。
 *
 * 相机与 ML Kit 相关实现已下沉到 `core:qrscanner`（feature 之间不能互相依赖，
 * 这类平台能力需要共享落点，Authenticator 的扫码导入也复用它）。
 * 本页只保留演示特有的部分：结果用 Snackbar 展示、右下角图片选择器入口。
 */
@Composable
fun BarcodeScanningScreen() {
    val pictureNavigator = LocalNavigators.current.picture
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()

    SnackbarHost(hostState = snackState, Modifier)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        QrCodeScannerContainer(
            modifier = Modifier.fillMaxSize(),
            onResult = { qrContent ->
                snackScope.launch {
                    snackState.showSnackbar(qrContent)
                }
            },
            onPermissionDenied = {
                snackScope.launch {
                    snackState.showSnackbar("相机权限被拒绝，请前往设置页面手动授权")
                }
            },
        )
        AssistChip(
            onClick = { pictureNavigator.openPictureSelector() },
            label = {},
            leadingIcon = {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = AssistChipDefaults.assistChipBorder(
                true,
                borderColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
