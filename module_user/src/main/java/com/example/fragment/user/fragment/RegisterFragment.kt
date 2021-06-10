package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.common.constant.NavMode
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentRegisterBinding
import com.example.fragment.user.model.UserViewModel

class RegisterFragment : ViewModelFragment<FragmentRegisterBinding, UserViewModel>() {

    override fun setViewBinding(): (LayoutInflater) -> FragmentRegisterBinding {
        return FragmentRegisterBinding::inflate
    }

    override fun setViewModel(): Class<UserViewModel> {
        return UserViewModel::class.java
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
        binding.repassword.addKeyboardListener(binding.root)
        binding.login.setOnClickListener { baseActivity.navigation(Router.MAIN, navMode = NavMode.POP_BACK_STACK) }
        binding.register.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            val repassword = binding.repassword.text.toString()
            if (checkParameter(username, password, repassword)) {
                viewModel.register(username, password, repassword)
            }
        }
    }

    private fun update() {
        viewModel.registerResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                result.data?.apply {
                    WanHelper.setUser(this)
                }
                baseActivity.navigation(Router.MAIN, navMode = NavMode.POP_BACK_STACK)
            }
            if (result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
    }

    private fun checkParameter(username: String, password: String, repassword: String): Boolean {
        if (username.isBlank()) {
            baseActivity.showTips("用户名不能为空")
            return false
        }
        if (password.isBlank()) {
            baseActivity.showTips("密码不能为空")
            return false
        }
        if (repassword.isBlank()) {
            baseActivity.showTips("确认密码不能为空")
            return false
        }
        if (password != repassword) {
            baseActivity.showTips("两次密码不一样")
            return false
        }
        return true
    }

}