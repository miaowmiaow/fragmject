package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.databinding.UserLoginFragmentBinding
import com.example.fragment.module.user.model.UserLoginViewModel
import com.example.fragment.module.user.model.UserViewModel

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
        binding.black.setOnClickListener { navigation(Router.MAIN) }
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
            navigation(Router.USER_REGISTER)
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

}