package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentUserLoginBinding
import com.example.fragment.module.user.model.UserLoginViewModel

class UserLoginFragment : RouterFragment() {

    private val viewModel: UserLoginViewModel by viewModels()
    private var _binding: FragmentUserLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
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
            activity.navigation(Router.USER_REGISTER)
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    WanHelper.setUser(result.data)
                    activity.onBackPressed()
                }
                else -> activity.showTips(result.errorMsg)
            }
        }
        return viewModel
    }

    override fun initLoad() {
    }

    private fun checkParameter(username: String, password: String): Boolean {
        if (username.isBlank()) {
            activity.showTips("用户名不能为空")
            return false
        }
        if (password.isBlank()) {
            activity.showTips("密码不能为空")
            return false
        }
        return true
    }

}