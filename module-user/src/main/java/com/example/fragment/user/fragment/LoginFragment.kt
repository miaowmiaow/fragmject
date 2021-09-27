package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentLoginBinding
import com.example.fragment.user.model.UserViewModel

class LoginFragment : RouterFragment() {

    private val viewModel: UserViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
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