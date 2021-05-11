package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.base.db.SimpleDBHelper
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.user.databinding.FragmentLoginBinding
import com.example.fragment.user.bean.UserBean
import com.example.fragment.user.model.LoginModel

class LoginFragment : ViewModelFragment<FragmentLoginBinding, LoginModel>() {

    override fun setViewBinding(inflater: LayoutInflater): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.login.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            if (checkParameter(username, password)) {
                viewModel.login(username, password)
            }
        }
    }

    private fun update() {
        viewModel.loginResult.observe(viewLifecycleOwner, {
            if (it.errorCode == "0") {
                it.data?.apply {
                    SimpleDBHelper.set(UserBean::class.java.simpleName, this.toJson())
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