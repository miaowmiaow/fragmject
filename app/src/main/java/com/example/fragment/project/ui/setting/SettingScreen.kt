package com.example.fragment.project.ui.setting

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.ArrowRightItem
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.NightSwitchButton
import com.example.fragment.project.components.TitleBar
import com.example.miaow.base.dialog.showStandardDialog
import com.example.miaow.base.utils.CacheUtils
import com.example.miaow.base.utils.CacheUtils.getTotalSize
import com.example.miaow.base.utils.FileUtil
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SettingScreen(
    viewModel: SettingViewModel = viewModel(),
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
//    var screenCaptureState by rememberSaveable { mutableStateOf(false) }
    var cacheSize by rememberSaveable { mutableStateOf("0KB") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        cacheSize = getTotalSize(context)
    }
    Scaffold(
        topBar = {
            TitleBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        content = { innerPadding ->
            LoadingContent(uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.user != null) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .fillMaxWidth()
                                .height(45.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "深色模式",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 25.dp, end = 25.dp),
                                fontSize = 13.sp,
                            )
                            NightSwitchButton(
                                checked = uiState.user?.darkTheme ?: false,
                                onCheckedChange = {
                                    viewModel.updateDarkTheme(it)
                                },
                                modifier = Modifier.size(52.dp, 32.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                    }
//                    HorizontalDivider()
//                    Row(
//                        modifier = Modifier
//                            .background(MaterialTheme.colorScheme.surfaceContainer)
//                            .fillMaxWidth()
//                            .height(45.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "屏幕录制",
//                            modifier = Modifier
//                                .weight(1f)
//                                .padding(start = 25.dp, end = 25.dp),
//                            fontSize = 13.sp,
//                        )
//                        Switch(
//                            checked = screenCaptureState,
//                            onCheckedChange = {
//                                screenCaptureState = it
//                                if (screenCaptureState) {
//                                    (context as AppCompatActivity).startScreenCapture(object :
//                                        ScreenCaptureCallback {
//                                        override fun onActivityResult(
//                                            resultCode: Int,
//                                            message: String
//                                        ) {
//                                            if (resultCode != Activity.RESULT_OK) {
//                                                screenCaptureState = false
//                                            }
//                                            scope.launch {
//                                                snackbarHostState.showSnackbar(
//                                                    message, "确定"
//                                                )
//                                            }
//                                        }
//                                    })
//                                } else {
//                                    (context as AppCompatActivity).stopScreenCapture()
//                                }
//                            },
//                        )
//                        Spacer(Modifier.width(5.dp))
//                    }
//                    HorizontalDivider()
//                    ArrowRightItem("跳过广告", "(仅支持部分APP的倒计时广告)") {
//                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
//                    }
//                    HorizontalDivider()
//                    ArrowRightItem("电池优化", "(跳过广告与我配合效果更佳哦~)") {
//                        context.requestIgnoreBatteryOptimizations()
//                    }
                    HorizontalDivider()
                    ArrowRightItem("隐私政策") { onNavigateToWeb("file:///android_asset/privacy_policy.html") }
                    HorizontalDivider()
                    ArrowRightItem("问题反馈") { onNavigateToWeb("https://github.com/miaowmiaow/fragmject/issues") }
                    HorizontalDivider()
                    ArrowRightItem("抹除数据") {
                        context.showStandardDialog(
                            content = "确定后将向手机写入脏数据，建议多次操作防止隐私泄露。",
                            confirm = {
                                FileUtil.writeDirtyRead(
                                    File(
                                        CacheUtils.getDirPath(context, "org"),
                                        "DirtyRead"
                                    )
                                )
                            }
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .clickable {
                                context.showStandardDialog(
                                    content = "确定要清除缓存吗？",
                                    confirm = {
                                        scope.launch {
                                            CacheUtils.clearAllCache(context)
                                            cacheSize = getTotalSize(context)
                                        }
                                    }
                                )
                            }
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .fillMaxWidth()
                            .height(45.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "清除缓存",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 25.dp, end = 25.dp),
                            fontSize = 13.sp,
                        )
                        Text(
                            text = cacheSize,
                            fontSize = 13.sp,
                        )
                        Image(
                            painter = painterResource(id = R.mipmap.ic_right),
                            contentDescription = "",
                            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
                        )
                    }
                    HorizontalDivider()
                    ArrowRightItem("关于玩Android") { onNavigateToWeb("https://wanandroid.com") }
                    Spacer(Modifier.height(20.dp))
                    if (uiState.user != null) {
                        Button(
                            onClick = {
                                viewModel.logout()
                            },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.dp, WanTheme.theme),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WanTheme.theme,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            contentPadding = PaddingValues(0.dp, 15.dp, 0.dp, 15.dp)
                        ) {
                            Text(
                                text = "退出登录",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SettingScreenPreview() {
    WanTheme { SettingScreen() }
}