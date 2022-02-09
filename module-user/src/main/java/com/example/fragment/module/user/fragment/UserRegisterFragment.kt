package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.UserRegisterFragmentBinding
import com.example.fragment.module.user.model.UserLoginViewModel

class UserRegisterFragment : RouterFragment() {

    private val viewModel: UserLoginViewModel by viewModels()
    private var _binding: UserRegisterFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserRegisterFragmentBinding.inflate(inflater, container, false)
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
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.username.addKeyboardListener(binding.root)
        binding.password.addKeyboardListener(binding.root)
        binding.repassword.addKeyboardListener(binding.root)
        binding.login.setOnClickListener { activity.onBackPressed() }
        binding.register.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            val rePassword = binding.repassword.text.toString()
            if (checkParameter(username, password, rePassword)) {
                viewModel.register(username, password, rePassword)
            }
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
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

    override fun initLoad() {}

    private fun checkParameter(username: String, password: String, rePassword: String): Boolean {
        if (username.isBlank()) {
            activity.showTips("用户名不能为空")
            return false
        }
        if (password.isBlank()) {
            activity.showTips("密码不能为空")
            return false
        }
        if (rePassword.isBlank()) {
            activity.showTips("确认密码不能为空")
            return false
        }
        if (password != rePassword) {
            activity.showTips("两次密码不一样")
            return false
        }
        return true
    }

}