package com.example.fragment.project.ui.register

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateUp: () -> Unit = {},
    onPopBackStackToMain: () -> Unit = {},
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()
    SideEffect {
        if (uiState.success) {
            onPopBackStackToMain()
        }
    }
    LaunchedEffect(uiState.message, scaffoldState.snackbarHostState) {
        if (uiState.message.isNotBlank()) {
            scaffoldState.snackbarHostState.showSnackbar(uiState.message)
            viewModel.resetMessage()
        }
    }
    var usernameText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }
    var againPasswordText by rememberSaveable { mutableStateOf("") }
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(it) { data -> Snackbar(snackbarData = data) } },
        content = { innerPadding ->
            LoadingContent(uiState.isLoading, innerPadding = innerPadding) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .paint(
                            painter = painterResource(id = R.mipmap.bg),
                            contentScale = ContentScale.FillBounds
                        )
                        .padding(start = 40.dp, top = 15.dp, end = 40.dp, bottom = 15.dp)
                        .verticalScroll(scrollState)
                        .systemBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_back),
                        contentDescription = null,
                        modifier = Modifier
                            .clickable { onNavigateUp() }
                            .size(15.dp)
                    )
                    Spacer(Modifier.height(30.dp))
                    Text(
                        text = "Create",
                        style = MaterialTheme.typography.h4,
                        color = colorResource(R.color.white),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.h5,
                        color = colorResource(R.color.white),
                    )
                    Spacer(Modifier.weight(1f))
                    WhiteTextField(
                        value = usernameText,
                        onValueChange = { usernameText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = { Text("请输入用户名") },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(15.dp))
                    WhiteTextField(
                        value = passwordText,
                        onValueChange = { passwordText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
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
                            .height(50.dp),
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
                    Spacer(Modifier.height(30.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "注册",
                            fontSize = 20.sp,
                            color = colorResource(R.color.white),
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                viewModel.register(
                                    usernameText,
                                    passwordText,
                                    againPasswordText
                                )
                            },
                            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = colorResource(R.color.theme_orange),
                                contentColor = colorResource(R.color.white)
                            ),
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
                        text = "去登录",
                        textDecoration = TextDecoration.Underline,
                        fontSize = 12.sp,
                        color = colorResource(R.color.white),
                        modifier = Modifier
                            .clickable {
                                if (context is AppCompatActivity) {
                                    context.onBackPressedDispatcher.onBackPressed()
                                }
                            }
                    )
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    )
}