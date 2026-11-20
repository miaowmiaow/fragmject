package com.example.fragmject.feature.auth.ui.register

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.WhiteSecureTextField
import com.example.fragmject.core.ui.components.WhiteTextField
import com.example.fragmject.feature.auth.components.AccountForm
import com.example.fragmject.feature.auth.components.AccountFormTexts

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val onNavigateUp = LocalOnNavigateUp.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = uiState is RegisterUiState.Loading
    val authNavigator = LocalNavigators.current.auth
    LaunchedEffect(uiState, snackbarHostState) {
        when (val s = uiState) {
            is RegisterUiState.Success -> {
                // onAuthSuccess 会触发本 entry 出栈，SnackbarHost 随组合销毁，
                // 故不在此显示 snackbar；跳转回首页即注册成功的反馈。
                // 先清状态再导航，避免状态残留导致重复触发。
                viewModel.resetMessage()
                authNavigator.onAuthSuccess()
            }
            is RegisterUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetMessage()
            }
            else -> {}
        }
    }
    // 表单文本刻意用 remember（不进 saved-state）：rememberSaveable 的值会写入
    // onSaveInstanceState 的 Bundle，进程被回收后由系统持久化到任务快照，
    // 明文密码会因此落到沙箱之外并延长生命周期。旋转等配置变更由 MainActivity 的
    // configChanges 自己处理，进程重建时重新输入是可接受的代价。
    //
    // 密码状态同样必须 `remember { TextFieldState() }`：`rememberTextFieldState()` 内部走
    // rememberSaveable，等于换一种方式把密码写回 saved-state。
    var usernameText by remember { mutableStateOf("") }
    val passwordState = remember { TextFieldState() }
    val againPasswordState = remember { TextFieldState() }
    val submit: () -> Unit = {
        viewModel.register(
            usernameText,
            passwordState.text.toString(),
            againPasswordState.text.toString(),
        )
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        content = { innerPadding ->
            AccountForm(
                texts = AccountFormTexts(
                    title = "Create",
                    subtitle = "Account",
                    buttonText = "注册",
                    bottomLinkText = "去登录",
                    isLoading = isLoading,
                ),
                onBack = onNavigateUp,
                onSubmit = submit,
                onBottomLinkClick = {
                    if (context is ComponentActivity) {
                        context.onBackPressedDispatcher.onBackPressed()
                    }
                },
                contentPadding = innerPadding,
            ) {
                RegisterFields(
                    username = usernameText,
                    password = passwordState,
                    againPassword = againPasswordState,
                    onUsernameChange = { usernameText = it },
                    onSubmit = {
                        submit()
                        keyboardController?.hide()
                    },
                )
            }
        }
    )
}

/** 用户名 / 密码 / 确认密码三项输入。 */
@Composable
private fun RegisterFields(
    username: String,
    password: TextFieldState,
    againPassword: TextFieldState,
    onUsernameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    WhiteTextField(
        value = username,
        onValueChange = onUsernameChange,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp),
        textStyle = TextStyle.Default.copy(fontSize = 14.sp, lineHeight = 1.4.em),
        placeholder = { Text("请输入用户名") },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
    )
    Spacer(Modifier.height(15.dp))
    WhiteSecureTextField(
        state = password,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp),
        textStyle = TextStyle.Default.copy(fontSize = 14.sp, lineHeight = 1.4.em),
        placeholder = { Text("请输入用户密码") },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
    )
    Spacer(Modifier.height(15.dp))
    WhiteSecureTextField(
        state = againPassword,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp),
        textStyle = TextStyle.Default.copy(fontSize = 14.sp, lineHeight = 1.4.em),
        placeholder = { Text("请再次输入密码") },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Go
        ),
        onKeyboardAction = KeyboardActionHandler { onSubmit() },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun RegisterScreenPreview() {
    AppTheme { RegisterScreen() }
}
