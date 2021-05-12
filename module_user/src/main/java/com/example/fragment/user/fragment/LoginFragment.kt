package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.utils.UserHelper
import com.example.fragment.module.user.databinding.FragmentLoginBinding
import com.example.fragment.user.model.UserModel

class LoginFragment : ViewModelFragment<FragmentLoginBinding, UserModel>() {

    override fun setViewBinding(inflater: LayoutInflater): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.username.addKeyboardListener(binding.root)
        binding.password.addKeyboardListener(binding.root)
        binding.login.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            if (checkParameter(username, password)) {
                viewModel.login(username, password)
            }
        }
        binding.register.setOnClickListener {
            baseActivity.navigation(Router.REGISTER)
        }
    }

    private fun update() {
        viewModel.loginResult.observe(viewLifecycleOwner, {
            if (it.errorCode == "0") {
                it.data?.apply {
                    UserHelper.setUser(this)
                }
                baseActivity.onBackPressed()
            } else {
                baseActivity.showTips(it.errorMsg)
            }
        })
    }

    private fun checkParameter(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            baseActivity.showTips("用户名不能为空")
            return false
        }
        if (password.isEmpty()) {
            baseActivity.showTips("密码不能为空")
            return false
        }
        return true
    }

}