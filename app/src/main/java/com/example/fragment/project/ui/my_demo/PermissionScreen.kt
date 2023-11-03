package com.example.fragment.project.ui.my_demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fragment.project.R

@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    else
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    var storageDialog by remember { mutableStateOf(false) }
    val requestPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ps ->
            var isGranted = true
            ps.entries.forEach {
                if (it.key in storagePermissions && !it.value)
                    isGranted = false
            }
            storageDialog = !isGranted
        }
    if (storageDialog) {
        AlertDialog(
            onDismissRequest = { storageDialog = false },
            title = { Text(text = "申请存储空间权限") },
            text = { Text(text = "玩Android需要使用存储空间，我们想要将文章内容缓存到本地，从而加快打开速度和减少用户流量使用") },
            confirmButton = {
                TextButton(onClick = { requestPermissions.launch(storagePermissions) }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { storageDialog = false }) { Text("取消") }
            }
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                storageDialog = !storagePermissions.all {
                    ContextCompat.checkSelfPermission(
                        context, it
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (!storageDialog) {
                    Toast.makeText(context, "存储权限已经获取", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.height(30.dp),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(1.dp, colorResource(R.color.theme_orange)),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colorResource(R.color.white),
                contentColor = colorResource(R.color.theme_orange)
            ),
            contentPadding = PaddingValues(3.dp, 2.dp, 3.dp, 2.dp)
        ) {
            Text(
                text = "存储权限demo",
                fontSize = 12.sp
            )
        }
    }
}