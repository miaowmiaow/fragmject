package com.example.fragment.project.ui.setting

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.dialog.StandardDialog
import com.example.fragment.library.base.utils.CacheUtils
import com.example.fragment.library.base.utils.ScreenRecordHelper.startScreenRecord
import com.example.fragment.library.base.utils.ScreenRecordHelper.stopScreenRecord
import com.example.fragment.project.R
import com.example.fragment.project.components.Loading
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.launch

@Composable
fun SettingScreen(
    viewModel: SettingViewModel = viewModel(),
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var systemThemeState by rememberSaveable { mutableStateOf(false) }
    var darkThemeState by rememberSaveable { mutableStateOf(false) }
    var screenRecordState by rememberSaveable { mutableStateOf(false) }
    var cacheSize by rememberSaveable { mutableStateOf(CacheUtils.getTotalSize(context)) }
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    DisposableEffect(LocalLifecycleOwner.current) {
        WanHelper.getUiMode {
            if (it == "-1") {
                systemThemeState = true
            } else if (it == "2") {
                darkThemeState = true
            }
        }
        onDispose {}
    }
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(it) { data ->
                Snackbar(
                    snackbarData = data
                )
            }
        },
        content = { innerPadding ->
            Loading(uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .background(colorResource(R.color.background))
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .background(colorResource(R.color.theme))
                    ) {
                        IconButton(
                            modifier = Modifier.height(45.dp),
                            onClick = {
                                if (context is AppCompatActivity) {
                                    context.onBackPressedDispatcher.onBackPressed()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = null,
                                tint = colorResource(R.color.white)
                            )
                        }
                        Text(
                            text = "设置",
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_fff),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .background(colorResource(R.color.white))
                            .fillMaxWidth()
                            .height(45.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "跟随系统深色模式",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 25.dp, end = 25.dp),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_333),
                        )
                        Switch(
                            checked = systemThemeState,
                            onCheckedChange = {
                                systemThemeState = it
                                if (systemThemeState) {
                                    WanHelper.setUiMode("-1")
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                } else {
                                    WanHelper.setUiMode("1")
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                }
                            }
                        )
                    }
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.line))
                            .fillMaxWidth()
                            .height(1.dp)
                    )
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
                        Switch(
                            checked = darkThemeState,
                            onCheckedChange = {
                                darkThemeState = it
                                if (darkThemeState) {
                                    WanHelper.setUiMode("2")
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                } else {
                                    WanHelper.setUiMode("1")
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                }
                            }
                        )
                    }
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.line))
                            .fillMaxWidth()
                            .height(1.dp)
                    )
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
                                    coroutineScope.launch {
                                        if (context is AppCompatActivity) {
                                            context.startScreenRecord { code, message ->
                                                if (code != Activity.RESULT_OK) {
                                                    screenRecordState = false
                                                }
                                                coroutineScope.launch {
                                                    scaffoldState.snackbarHostState.showSnackbar(
                                                        message
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (context is AppCompatActivity) {
                                        context.stopScreenRecord()
                                        coroutineScope.launch {
                                            scaffoldState.snackbarHostState.showSnackbar("屏幕录制结束")
                                        }
                                    }
                                }
                            }
                        )
                    }
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.line))
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                    Row(
                        modifier = Modifier
                            .background(colorResource(R.color.white))
                            .fillMaxWidth()
                            .height(45.dp)
                            .clickable {
                                onNavigateToPrivacyPolicy()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "隐私政策",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 25.dp, end = 25.dp),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_333),
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = "",
                            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
                        )
                    }
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.line))
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                    Row(
                        modifier = Modifier
                            .background(colorResource(R.color.white))
                            .fillMaxWidth()
                            .height(45.dp)
                            .clickable {
                                onNavigateToFeedback()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "问题反馈",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 25.dp, end = 25.dp),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_333),
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = "",
                            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
                        )
                    }
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.line))
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                    Row(
                        modifier = Modifier
                            .background(colorResource(R.color.white))
                            .fillMaxWidth()
                            .height(45.dp)
                            .clickable {
                                onNavigateToAbout()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "关于玩Android",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 25.dp, end = 25.dp),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_333),
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = "",
                            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
                        )
                    }
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.line))
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                    Row(
                        modifier = Modifier
                            .background(colorResource(R.color.white))
                            .fillMaxWidth()
                            .height(45.dp)
                            .clickable {
                                if (context is AppCompatActivity) {
                                    StandardDialog
                                        .newInstance()
                                        .setContent("确定要清除缓存吗？")
                                        .setOnDialogClickListener(object :
                                            StandardDialog.OnDialogClickListener {
                                            override fun onConfirm(dialog: StandardDialog) {
                                                CacheUtils.clearAllCache(context)
                                                cacheSize = CacheUtils.getTotalSize(context)
                                            }

                                            override fun onCancel(dialog: StandardDialog) {
                                            }
                                        })
                                        .show(context.supportFragmentManager)
                                }
                            },
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
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = "",
                            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
                        )
                    }
                    Spacer(
                        Modifier
                            .background(colorResource(R.color.line))
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                    if (uiState.userBean.id.isNotBlank()) {
                        Button(
                            onClick = {
                                if (context is AppCompatActivity) {
                                    StandardDialog.newInstance()
                                        .setContent("确定退出登录吗？")
                                        .setOnDialogClickListener(object :
                                            StandardDialog.OnDialogClickListener {
                                            override fun onConfirm(dialog: StandardDialog) {
                                                viewModel.logout()
                                            }

                                            override fun onCancel(dialog: StandardDialog) {
                                            }

                                        })
                                        .show(context.supportFragmentManager)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.dp, colorResource(R.color.theme)),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = colorResource(R.color.theme),
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