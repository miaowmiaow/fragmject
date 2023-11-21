package com.example.fragment.project.ui.setting

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.ArrowRightItem
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.NightSwitchButton
import com.example.fragment.project.components.TitleBar
import com.example.miaow.base.dialog.showStandardDialog
import com.example.miaow.base.utils.CacheUtils
import com.example.miaow.base.utils.CacheUtils.getTotalSize
import com.example.miaow.base.utils.ScreenRecordCallback
import com.example.miaow.base.utils.startScreenRecord
import com.example.miaow.base.utils.stopScreenRecord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    viewModel: SettingViewModel = viewModel(),
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var screenRecordState by rememberSaveable { mutableStateOf(false) }
    var cacheSize by rememberSaveable { mutableStateOf("0KB") }
    val coroutineScope = rememberCoroutineScope()
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
                            tint = colorResource(R.color.white)
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
                    Row(
                        modifier = Modifier
                            .background(colorResource(R.color.white))
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
                            color = colorResource(R.color.text_333),
                        )
                        NightSwitchButton(
                            checked = uiState.darkTheme,
                            onCheckedChange = {
                                viewModel.updateUiMode(it)
                                if (it) {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                } else {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                }
                            },
                            modifier = Modifier.size(52.dp, 32.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .background(colorResource(R.color.white))
                            .fillMaxWidth()
                            .height(45.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "屏幕录制",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 25.dp, end = 25.dp),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_333),
                        )
                        Switch(
                            checked = screenRecordState,
                            onCheckedChange = {
                                screenRecordState = it
                                if (screenRecordState) {
                                    (context as AppCompatActivity).startScreenRecord(object :
                                        ScreenRecordCallback {
                                        override fun onActivityResult(
                                            resultCode: Int,
                                            message: String
                                        ) {
                                            if (resultCode != Activity.RESULT_OK) {
                                                screenRecordState = false
                                            }
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message, "确定"
                                                )
                                            }
                                        }
                                    })
                                } else {
                                    (context as AppCompatActivity).stopScreenRecord(object :
                                        ScreenRecordCallback {
                                        override fun onActivityResult(
                                            resultCode: Int,
                                            message: String
                                        ) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message, "确定"
                                                )
                                            }
                                        }
                                    })
                                }
                            },
                        )
                        Spacer(Modifier.width(5.dp))
                    }
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
                    Row(
                        modifier = Modifier
                            .clickable {
                                context.showStandardDialog(
                                    content = "确定要清除缓存吗？",
                                    confirm = {
                                        coroutineScope.launch {
                                            CacheUtils.clearAllCache(context)
                                            cacheSize = getTotalSize(context)
                                        }
                                    }
                                )
                            }
                            .background(colorResource(R.color.white))
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
                            color = colorResource(R.color.text_333),
                        )
                        Text(
                            text = cacheSize,
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_333),
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
                    if (uiState.userBean.id.isNotBlank()) {
                        Button(
                            onClick = {
                                context.showStandardDialog(
                                    content = "确定退出登录吗？",
                                    confirm = {
                                        viewModel.logout()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.dp, colorResource(R.color.theme)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.theme),
                                contentColor = colorResource(R.color.white)
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