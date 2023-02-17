package com.example.fragment.project.ui.register

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.WhiteTextField
import com.example.fragment.project.ui.main.user.UserViewModel

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by registerViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.result.errorCode) {
        when (uiState.result.errorCode) {
            "0" -> {
                userViewModel.updateUserBean(uiState.result.data)
                if (context is AppCompatActivity) {
                    context.onBackPressedDispatcher.onBackPressed()
                }
            }
            else -> {
                if (context is AppCompatActivity && uiState.result.errorMsg.isNotBlank()) {
                    Toast.makeText(context, uiState.result.errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    var usernameText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }
    var againPasswordText by rememberSaveable { mutableStateOf("") }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = R.drawable.bg),
                contentScale = ContentScale.FillBounds
            )
            .padding(start = 40.dp, end = 40.dp)
            .systemBarsPadding()
    ) {
        val (black, welcome, wan, username, password, againPassword, register, sign_in, sign_up) = createRefs()
        val registerTopBarrier = createTopBarrier(register)
        val registerBottomBarrier = createBottomBarrier(register)
        Image(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = null,
            modifier = Modifier
                .size(15.dp)
                .constrainAs(black) {
                    top.linkTo(parent.top, margin = 15.dp)
                }
                .clickable {
                    if (context is AppCompatActivity) {
                        context.onBackPressedDispatcher.onBackPressed()
                    }
                }
        )
        Text(
            text = "Create",
            style = MaterialTheme.typography.h4,
            color = colorResource(R.color.white),
            modifier = Modifier
                .constrainAs(welcome) {
                    top.linkTo(black.bottom, 30.dp)
                }
        )
        Text(
            text = "Account",
            style = MaterialTheme.typography.h5,
            color = colorResource(R.color.white),
            modifier = Modifier
                .constrainAs(wan) {
                    top.linkTo(welcome.bottom, 10.dp)
                }
        )
        WhiteTextField(
            value = usernameText,
            onValueChange = {
                usernameText = it
            },
            placeholder = {
                Text("请输入用户名")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .constrainAs(username) {
                    bottom.linkTo(password.top, 10.dp)
                }
        )
        WhiteTextField(
            value = passwordText,
            onValueChange = {
                passwordText = it
            },
            placeholder = {
                Text("请输入用户密码")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .constrainAs(password) {
                    bottom.linkTo(againPassword.top, 10.dp)
                }
        )
        WhiteTextField(
            value = againPasswordText,
            onValueChange = {
                againPasswordText = it
            },
            placeholder = {
                Text("请再次输入密码")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .constrainAs(againPassword) {
                    bottom.linkTo(register.top, 15.dp)
                }
        )
        Text(
            text = "注册",
            fontSize = 24.sp,
            color = colorResource(R.color.white),
            modifier = Modifier
                .constrainAs(sign_up) {
                    top.linkTo(registerTopBarrier)
                    bottom.linkTo(registerBottomBarrier)
                }
        )
        Button(
            onClick = {
                if (usernameText.isBlank()) {
                    Toast.makeText(context, "用户名不能为空", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (passwordText.isBlank()) {
                    Toast.makeText(context, "密码不能为空", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (againPasswordText.isBlank()) {
                    Toast.makeText(context, "确认密码不能为空", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (passwordText != againPasswordText) {
                    Toast.makeText(context, "两次密码不一样", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                registerViewModel.register(usernameText, passwordText, againPasswordText)
            },
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colorResource(R.color.theme_orange),
                contentColor = colorResource(R.color.white)
            ),
            contentPadding = PaddingValues(15.dp),
            modifier = Modifier
                .size(55.dp)
                .constrainAs(register) {
                    end.linkTo(parent.end)
                    bottom.linkTo(sign_in.top, 40.dp)
                }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_right_arrow),
                contentDescription = null
            )
        }
        Text(
            text = "去登录",
            textDecoration = TextDecoration.Underline,
            fontSize = 16.sp,
            color = colorResource(R.color.white),
            modifier = Modifier
                .constrainAs(sign_in) {
                    bottom.linkTo(parent.bottom, 40.dp)
                }
                .clickable {
                    if (context is AppCompatActivity) {
                        context.onBackPressedDispatcher.onBackPressed()
                    }
                }
        )
    }
}