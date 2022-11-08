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
import com.example.fragment.library.base.theme.WanTheme
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.model.UserLoginViewModel
import com.example.fragment.module.user.model.UserViewModel
import com.example.fragment.module.user.view.WhiteTextField

class UserLoginFragment : RouterFragment() {

    private val viewModel: UserLoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    UserLoginPage()
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
        viewModel.loginResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) { bean ->
                bean.data?.let { data ->
                    val userViewModel: UserViewModel by activityViewModels()
                    userViewModel.updateUserBean(data)
                }
                onBackPressed()
            }
        }
        return viewModel
    }

    fun checkParameter(username: String, password: String): Boolean {
        if (username.isBlank()) {
            showTips("用户名不能为空")
            return false
        }
        if (password.isBlank()) {
            showTips("密码不能为空")
            return false
        }
        return true
    }

    @Composable
    fun UserLoginPage() {
        var usernameText by rememberSaveable { mutableStateOf("") }
        var passwordText by rememberSaveable { mutableStateOf("") }
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
            val (black, welcome, wan, username, password, login, sign_in, sign_up) = createRefs()
            val loginTopBarrier = createTopBarrier(login)
            val loginBottomBarrier = createBottomBarrier(login)
            Image(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = null,
                modifier = Modifier
                    .size(15.dp)
                    .constrainAs(black) {
                        top.linkTo(parent.top, margin = 15.dp)
                    }
                    .clickable {
                        navigation(Router.MAIN)
                    }
            )
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.h4,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(welcome) {
                        top.linkTo(black.bottom, 60.dp)
                    }
            )
            Text(
                text = "玩Android",
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
                        bottom.linkTo(login.top, 25.dp)
                    }
            )
            Text(
                text = "登录",
                fontSize = 25.sp,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(sign_in) {
                        top.linkTo(loginTopBarrier)
                        bottom.linkTo(loginBottomBarrier)
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
                    .constrainAs(login) {
                        end.linkTo(parent.end)
                        bottom.linkTo(sign_up.top, 60.dp)
                    }
                    .clickable {
                        if (checkParameter(usernameText, passwordText)) {
                            viewModel.login(usernameText, passwordText)
                        }
                    }
            )
            Text(
                text = "去注册",
                textDecoration = TextDecoration.Underline,
                fontSize = 16.sp,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(sign_up) {
                        bottom.linkTo(parent.bottom, 40.dp)
                    }
                    .clickable {
                        navigation(Router.USER_REGISTER)
                    }
            )
        }
    }

}