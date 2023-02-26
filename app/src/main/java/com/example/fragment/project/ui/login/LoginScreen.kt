package com.example.fragment.project.ui.login

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.fragment.project.components.Loading
import com.example.fragment.project.components.WhiteTextField
import com.example.fragment.project.utils.WanHelper

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onNavigateToRegister: () -> Unit = {},
    onPopBackStackToMain: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(uiState.result.errorCode) {
        when (uiState.result.errorCode) {
            "0" -> {
                uiState.result.data?.let {
                    WanHelper.setUser(it)
                }
                onPopBackStackToMain()
            }
            else -> {
                if (context is AppCompatActivity && uiState.result.errorMsg.isNotBlank()) {
                    Toast.makeText(context, uiState.result.errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        onDispose { }
    }
    var usernameText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }
    fun login() {
        if (usernameText.isBlank()) {
            Toast.makeText(context, "用户名不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (passwordText.isBlank()) {
            Toast.makeText(context, "密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        loginViewModel.login(usernameText, passwordText)
    }
    Loading(uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(id = R.drawable.bg),
                    contentScale = ContentScale.FillBounds
                )
                .padding(start = 40.dp, top = 15.dp, end = 40.dp, bottom = 15.dp)
                .systemBarsPadding()
                .navigationBarsPadding()
        ) {
            Image(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = null,
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onPopBackStackToMain() }
            )
            Spacer(Modifier.height(30.dp))
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.h4,
                color = colorResource(R.color.white),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "玩Android",
                style = MaterialTheme.typography.h6,
                color = colorResource(R.color.white),
            )
            Spacer(Modifier.weight(1f))
            WhiteTextField(
                value = usernameText,
                onValueChange = { usernameText = it },
                placeholder = { Text("请输入用户名") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { login() }),
            )
            Spacer(Modifier.height(30.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "登录",
                    fontSize = 20.sp,
                    color = colorResource(R.color.white)
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { login() },
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
                        painter = painterResource(R.drawable.ic_right_arrow),
                        contentDescription = null
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            Text(
                text = "去注册",
                textDecoration = TextDecoration.Underline,
                fontSize = 12.sp,
                color = colorResource(R.color.white),
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

