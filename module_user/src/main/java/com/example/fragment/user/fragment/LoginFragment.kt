package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentLoginBinding
import com.example.fragment.user.model.UserViewModel

class LoginFragment : ViewModelFragment<FragmentLoginBinding, UserViewModel>() {

    override fun setViewBinding(inflater: LayoutInflater): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.black.setOnClickListener { baseActivity.onBackPressed() }
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
        viewModel.loginResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.apply {
                    WanHelper.setUser(this)
                }
                baseActivity.onBackPressed()
            }
            if (result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
    }

    private fun checkParameter(username: String, password: String): Boolean {
        if (username.isBlank()) {
            baseActivity.showTips("用户名不能为空")
            return false
        }
        if (password.isBlank()) {
            baseActivity.showTips("密码不能为空")
            return false
        }
        return true
    }

}