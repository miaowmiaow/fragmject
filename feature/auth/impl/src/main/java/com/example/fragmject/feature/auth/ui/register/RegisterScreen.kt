package com.example.fragmject.feature.auth.ui.register

import androidx.navigation3.runtime.NavKey
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.WhiteTextField
import com.example.fragmject.feature.auth.components.AccountForm
import com.example.fragmject.feature.home.MainNavKey

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateUp: () -> Unit = {},
    onPopBackStack: (key: NavKey) -> Unit = {},
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = uiState is RegisterUiState.Loading
    LaunchedEffect(uiState, snackbarHostState) {
        when (val s = uiState) {
            is RegisterUiState.Success -> {
                onPopBackStack(MainNavKey)
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetMessage()
            }
            is RegisterUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetMessage()
            }
            else -> {}
        }
    }
    var usernameText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }
    var againPasswordText by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        content = { innerPadding ->
            AccountForm(
                title = "Create",
                subtitle = "Account",
                buttonText = "注册",
                bottomLinkText = "去登录",
                isLoading = isLoading,
                onBack = onNavigateUp,
                onSubmit = {
                    viewModel.register(
                        usernameText,
                        passwordText,
                        againPasswordText
                    )
                },
                onBottomLinkClick = {
                    if (context is ComponentActivity) {
                        context.onBackPressedDispatcher.onBackPressed()
                    }
                },
                contentPadding = innerPadding,
            ) {
                WhiteTextField(
                    value = usernameText,
                    onValueChange = { usernameText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp),
                    textStyle = TextStyle.Default.copy(fontSize = 14.sp, lineHeight = 14.sp),
                    placeholder = { Text("请输入用户名") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(15.dp))
                WhiteTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp),
                    textStyle = TextStyle.Default.copy(fontSize = 14.sp, lineHeight = 14.sp),
                    placeholder = { Text("请输入用户密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                )
                Spacer(Modifier.height(15.dp))
                WhiteTextField(
                    value = againPasswordText,
                    onValueChange = { againPasswordText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp),
                    textStyle = TextStyle.Default.copy(fontSize = 14.sp, lineHeight = 14.sp),
                    placeholder = { Text("请再次输入密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = {
                        viewModel.register(
                            usernameText,
                            passwordText,
                            againPasswordText
                        )
                        keyboardController?.hide()
                    }),
                )
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun RegisterScreenPreview() {
    AppTheme { RegisterScreen() }
}