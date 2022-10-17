package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.UserLoginFragmentBinding
import com.example.fragment.module.user.model.UserLoginViewModel
import com.example.fragment.module.user.model.UserViewModel
import com.example.fragment.module.user.view.WanTextField

class UserLoginFragment : RouterFragment() {

    private val viewModel: UserLoginViewModel by viewModels()
    private var _binding: UserLoginFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserLoginFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        hideInputMethod()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.composeView.setContent {
            MaterialTheme {
                UserLoginPage()
            }
        }
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
        ConstraintLayout {
            val (black, welcome, wan, username, password, login, sign_in, sign_up, forgot_passwords) = createRefs()
            val topBarrier = createTopBarrier(login)
            val bottomBarrier = createBottomBarrier(login)
            val startGuideline = createGuidelineFromStart(40.dp)
            val endGuideline = createGuidelineFromEnd(40.dp)
            Image(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .padding(15.dp)
                    .constrainAs(black) {
                        top.linkTo(parent.top)
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
                        top.linkTo(black.bottom, 40.dp)
                        start.linkTo(startGuideline)
                    }
            )
            Text(
                text = "玩Android",
                style = MaterialTheme.typography.h5,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(wan) {
                        top.linkTo(welcome.bottom, 10.dp)
                        start.linkTo(startGuideline)
                    }
            )
            WanTextField(
                value = usernameText,
                onValueChange = {
                    usernameText = it
                },
                placeholder = {
                    Text("Username")
                },
                modifier = Modifier
                    .height(50.dp)
                    .constrainAs(username) {
                        bottom.linkTo(password.top, 40.dp)
                        start.linkTo(startGuideline)
                        end.linkTo(endGuideline)
                    }
            )
            WanTextField(
                value = passwordText,
                onValueChange = {
                    passwordText = it
                },
                placeholder = {
                    Text("Password")
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .height(50.dp)
                    .constrainAs(password) {
                        bottom.linkTo(login.top, 40.dp)
                        start.linkTo(startGuideline)
                        end.linkTo(endGuideline)
                    }
            )
            Text(
                text = "Sign in",
                fontSize = 25.sp,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(sign_in) {
                        top.linkTo(topBarrier)
                        bottom.linkTo(bottomBarrier)
                        start.linkTo(startGuideline)
                    }
            )
            Image(
                painter = painterResource(R.drawable.ic_right_arrow),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colorResource(R.color.theme_orange))
                    .size(80.dp)
                    .padding(25.dp)
                    .constrainAs(login) {
                        bottom.linkTo(forgot_passwords.top, 80.dp)
                        end.linkTo(endGuideline)
                    }
                    .clickable {
                        if (checkParameter(usernameText, passwordText)) {
                            viewModel.login(usernameText, passwordText)
                        }
                    }
            )
            Text(
                text = "Sign up",
                textDecoration = TextDecoration.Underline,
                fontSize = 18.sp,
                color = colorResource(R.color.white),
                modifier = Modifier
                    .constrainAs(sign_up) {
                        bottom.linkTo(parent.bottom, 50.dp)
                        start.linkTo(startGuideline)
                    }
                    .clickable {
                        navigation(Router.USER_REGISTER)
                    }
            )
            Text(
                text = "Forgot Passwords",
                textDecoration = TextDecoration.Underline,
                fontSize = 18.sp,
                color = colorResource(id = R.color.white),
                modifier = Modifier
                    .constrainAs(forgot_passwords) {
                        bottom.linkTo(parent.bottom, margin = 50.dp)
                        end.linkTo(endGuideline)
                    }
                    .clickable {
                        showTips("敬请期待")
                    }
            )
        }
    }
}