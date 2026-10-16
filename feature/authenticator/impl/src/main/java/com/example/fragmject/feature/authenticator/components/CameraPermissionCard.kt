package com.example.fragmject.feature.authenticator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 相机权限未授予时的引导卡片：说明用途 + 再次申请按钮。 */
@Composable
fun CameraPermissionCard(
    onRequest: () -> Unit,
    onBackToManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("需要相机权限", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "扫描二维码需要相机权限，用于读取验证器导出的二维码，不会保存或上传画面",
            fontSize = 13.sp,
            color = AuthenticatorColors.secondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuthenticatorColors.accent,
                // accent 是浅色强调色，内容色须用深字浅底的 onAccent（勿用 Color.White，对比度不足）
                contentColor = AuthenticatorColors.onAccent,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("授予相机权限") }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onBackToManual,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = AuthenticatorColors.secondaryText,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("改用手动输入") }
    }
}
