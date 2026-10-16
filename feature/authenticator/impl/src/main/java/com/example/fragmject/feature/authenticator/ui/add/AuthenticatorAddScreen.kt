package com.example.fragmject.feature.authenticator.ui.add

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.domain.otp.OtpAlgorithm
import com.example.fragmject.core.domain.otp.OtpType
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.qrscanner.QrCodeScannerContainer
import com.example.fragmject.feature.authenticator.components.AnimatedTabBar
import com.example.fragmject.feature.authenticator.components.AuthenticatorAppBar
import com.example.fragmject.feature.authenticator.components.CameraPermissionCard
import com.example.fragmject.feature.authenticator.components.OptionButton
import com.example.fragmject.feature.authenticator.components.ScanFrameOverlay
import com.example.fragmject.feature.authenticator.components.ScanLineAnimation
import com.example.fragmject.feature.authenticator.components.ScanStatusIndicator
import com.example.fragmject.feature.authenticator.model.ScanState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** 新增方式：扫码导入或手输密钥。 */
private enum class AddMode(val title: String) { SCAN("扫码添加"), MANUAL("手动输入") }

@Composable
fun AuthenticatorAddScreen(
    viewModel: AuthenticatorAddViewModel = hiltViewModel(),
) {
    // 跟随 app 当前的亮/暗方案（见 AuthenticatorScreen 同款注释）
    AuthenticatorAddScreenContent(viewModel = viewModel)
}

@Composable
private fun AuthenticatorAddScreenContent(
    viewModel: AuthenticatorAddViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onNavigateUp = LocalOnNavigateUp.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 默认进扫码：绝大多数场景来自二维码，手输是兜底
    var mode by remember { mutableStateOf(AddMode.SCAN) }
    var scanState by remember { mutableStateOf<ScanState>(ScanState.Scanning) }
    var switchToManualAfterFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthenticatorAddEvent.Saved -> onNavigateUp()
                is AuthenticatorAddEvent.Imported -> {
                    snackbarHostState.showSnackbar("已导入 ${event.count} 个账户")
                    onNavigateUp()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AuthenticatorAppBar(title = "添加账户", onBackClick = onNavigateUp) },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedTabBar(
                selectedTab = if (mode == AddMode.SCAN) 0 else 1,
                onTabSelected = { mode = if (it == 0) AddMode.SCAN else AddMode.MANUAL },
                tabs = AddMode.entries.map { it.title },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        (slideInHorizontally(tween(250)) { it } + fadeIn(tween(250))) togetherWith
                            (slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(250)) { -it } + fadeIn(tween(250))) togetherWith
                            (slideOutHorizontally(tween(250)) { it } + fadeOut(tween(200)))
                    }
                },
                label = "addMode",
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                when (current) {
                    AddMode.SCAN -> ScannerSection(
                        onResult = { raw ->
                            when (viewModel.applyScan(raw)) {
                                ScanOutcome.Filled -> {
                                    scanState = ScanState.Success
                                    // 先给「识别成功」的反馈，再切到表单让用户确认
                                    switchToManualAfterFeedback = true
                                }
                                is ScanOutcome.Imported -> scanState = ScanState.Success
                                ScanOutcome.Unrecognized -> {
                                    scanState = ScanState.Error("无法识别的二维码")
                                    scope.launch { snackbarHostState.showSnackbar("无法识别的二维码，请改用手动输入") }
                                }
                            }
                        },
                        scanState = scanState,
                        onErrorShown = { scanState = ScanState.Scanning },
                        onBackToManual = { mode = AddMode.MANUAL },
                    )
                    AddMode.MANUAL -> ManualForm(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }

    LaunchedEffect(scanState) {
        if (switchToManualAfterFeedback && scanState is ScanState.Success) {
            delay(600.milliseconds)
            switchToManualAfterFeedback = false
            mode = AddMode.MANUAL
        }
    }
}

/**
 * 扫码区（交互对齐 HugeRivers/Authenticator）：
 * 遮罩取景框 + 扫描线 + 居中状态指示器 + 底部说明；权限未授予时显示引导卡片。
 *
 * 权限判断放在 feature：引擎只在「已授权」时被渲染，
 * 这样未授权时整块换成引导卡片，也不会出现「引擎静默什么都不画」的黑屏。
 */
@Composable
private fun ScannerSection(
    scanState: ScanState,
    onResult: (String) -> Unit,
    onErrorShown: () -> Unit,
    onBackToManual: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        CameraPermissionCard(
            onRequest = { launcher.launch(Manifest.permission.CAMERA) },
            onBackToManual = onBackToManual,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        QrCodeScannerContainer(
            modifier = Modifier.fillMaxSize(),
            drawBoundingBox = false,
            overlay = {
                ScanFrameOverlay(state = scanState)
                if (scanState is ScanState.Scanning) {
                    ScanLineAnimation()
                }
            },
            onResult = onResult,
            onPermissionDenied = { hasPermission = false },
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ScanStatusIndicator(state = scanState, onErrorShown = onErrorShown)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 32.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "将二维码放入取景框，自动识别",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "支持 otpauth:// 与 Google 验证器导出的批量码",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 手输表单。
 *
 * 输入框用 M3 `OutlinedTextField`（主题感知的自带配色）而不是固定文字色的
 * `WhiteTextField`：固定色会在主题切换时出现「浅字浅底」（真机验收发现）。
 */
@Composable
private fun ManualForm(
    uiState: AuthenticatorAddUiState,
    viewModel: AuthenticatorAddViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = uiState.issuer,
            onValueChange = viewModel::updateIssuer,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("发行方（可选）") },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.accountName,
            onValueChange = viewModel::updateAccountName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("账户名") },
            isError = uiState.nameError != null,
            supportingText = { uiState.nameError?.let { Text(it) } },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.secret,
            onValueChange = viewModel::updateSecret,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("密钥（Base32）") },
            isError = uiState.secretError != null,
            supportingText = {
                Text(uiState.secretError ?: "通常由服务方提供，仅含 A-Z 与 2-7")
            },
            singleLine = true,
        )
        Text(text = "算法", fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        Row {
            OtpAlgorithm.entries.forEach { algorithm ->
                OptionButton(
                    text = algorithm.wireValue,
                    selected = uiState.algorithm == algorithm,
                    onClick = { viewModel.updateAlgorithm(algorithm) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(5.dp))
            }
        }
        Text(text = "位数", fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        Row {
            listOf(6, 8).forEach { digits ->
                OptionButton(
                    text = "$digits 位",
                    selected = uiState.digits == digits,
                    onClick = { viewModel.updateDigits(digits) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(5.dp))
            }
        }
        if (uiState.type == OtpType.TOTP) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.periodText,
                onValueChange = viewModel::updatePeriod,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("周期（秒，默认 30）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        } else {
            Text(
                text = "HOTP 账户：基于计数器，UI 不提供推进",
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::save,
            enabled = !uiState.saving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            if (uiState.saving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    trackColor = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(text = "添加账户", fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
