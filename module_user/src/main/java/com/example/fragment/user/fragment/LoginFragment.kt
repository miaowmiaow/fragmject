package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.user.databinding.FragmentLoginBinding

class LoginFragment : ViewModelFragment<FragmentLoginBinding, BaseViewModel>(){

    override fun setViewBinding(inflater: LayoutInflater): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}