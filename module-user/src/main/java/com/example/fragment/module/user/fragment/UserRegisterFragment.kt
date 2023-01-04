package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.model.UserLoginViewModel
import com.example.fragment.module.user.model.UserViewModel
import com.example.fragment.module.user.view.WhiteTextField

class UserRegisterFragment : RouterFragment() {

    private val viewModel: UserLoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    UserRegisterPage()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    override fun initView() {
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.registerResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) { bean ->
                bean.data?.let { data ->
                    val userViewModel: UserViewModel by activityViewModels()
                    userViewModel.updateUserBean(data)
                }
                navigation(Router.MAIN)
            }
        }
        return viewModel
    }

    private fun checkParameter(username: String, password: String, againPassword: String): Boolean {
        if (username.isBlank()) {
            showTips("用户名不能为空")
            return false
        }
        if (password.isBlank()) {
            showTips("密码不能为空")
            return false
        }
        if (againPassword.isBlank()) {
            showTips("确认密码不能为空")
            return false
        }
        if (password != againPassword) {
            showTips("两次密码不一样")
            return false
        }
        return true
    }

    @Composable
    fun UserRegisterPage() {
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
                        onBackPressed()
                    }
            )
            Text(
                text = "Create",
                style = MaterialTheme.typography.h4,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(welcome) {
                        top.linkTo(black.bottom, 60.dp)
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
                        bottom.linkTo(password.top, 25.dp)
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
                        bottom.linkTo(againPassword.top, 25.dp)
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
                        bottom.linkTo(register.top, 25.dp)
                    }
            )
            Text(
                text = "注册",
                fontSize = 25.sp,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(sign_up) {
                        top.linkTo(registerTopBarrier)
                        bottom.linkTo(registerBottomBarrier)
                    }
            )
            Image(
                painter = painterResource(R.drawable.ic_right_arrow),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colorResource(R.color.theme_orange))
                    .size(75.dp)
                    .padding(25.dp)
                    .constrainAs(register) {
                        end.linkTo(parent.end)
                        bottom.linkTo(sign_in.top, 60.dp)
                    }
                    .clickable {
                        if (checkParameter(usernameText, passwordText, againPasswordText)) {
                            viewModel.register(usernameText, passwordText, againPasswordText)
                        }
                    }
            )
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
                        onBackPressed()
                    }
            )
        }
    }

}