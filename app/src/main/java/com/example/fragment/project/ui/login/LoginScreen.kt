package com.example.fragment.project.ui.login

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.WhiteTextField

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateToRegister: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onPopBackStackToMain: () -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    SideEffect {
        if (uiState.success) {
            onPopBackStackToMain()
        }
    }
    LaunchedEffect(uiState.message, snackbarHostState) {
        if (uiState.message.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.message)
            viewModel.resetMessage()
        }
    }
    var usernameText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        content = { innerPadding ->
            LoadingContent(
                isLoading = uiState.isLoading,
                innerPadding = innerPadding
            ) {
                Column(
                    modifier = Modifier
                        .paint(
                            painter = painterResource(id = R.mipmap.bg),
                            contentScale = ContentScale.FillBounds
                        )
                        .fillMaxSize()
                        .padding(15.dp)
                        .verticalScroll(scrollState)
                ) {
                    IconButton(
                        modifier = Modifier.height(45.dp),
                        onClick = onNavigateUp
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = colorResource(R.color.white)
                        )
                    }
                    Spacer(Modifier.height(30.dp))
                    Text(
                        text = "Welcome",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorResource(R.color.white),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "玩Android",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(R.color.white),
                    )
                    Spacer(Modifier.weight(1f))
                    WhiteTextField(
                        value = usernameText,
                        onValueChange = { usernameText = it },
                        textStyle = TextStyle.Default.copy(fontSize = 14.sp, lineHeight = 14.sp),
                        placeholder = { Text("请输入用户名") },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 20.dp)
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
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                viewModel.login(usernameText, passwordText)
                                keyboardController?.hide()
                            }
                        ),
                    )
                    Spacer(Modifier.height(30.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "登录",
                            fontSize = 20.sp,
                            color = colorResource(R.color.white)
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { viewModel.login(usernameText, passwordText) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.theme_orange),
                                contentColor = colorResource(R.color.white)
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                            contentPadding = PaddingValues(15.dp),
                            modifier = Modifier.size(55.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.mipmap.ic_right_arrow),
                                contentDescription = null
                            )
                        }
                    }
                    Spacer(Modifier.height(30.dp))
                    Text(
                        text = "去注册",
                        modifier = Modifier
                            .clickable { onNavigateToRegister() }
                            .padding(horizontal = 25.dp),
                        textDecoration = TextDecoration.Underline,
                        fontSize = 12.sp,
                        color = colorResource(R.color.white),
                    )
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    )
}

