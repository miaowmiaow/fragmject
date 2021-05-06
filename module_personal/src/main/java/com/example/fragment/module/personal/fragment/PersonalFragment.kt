package com.example.fragment.module.personal.fragment

import android.view.LayoutInflater
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.personal.databinding.FragmentPersonalBinding

class PersonalFragment : ViewModelFragment<FragmentPersonalBinding, BaseViewModel>() {

    override fun setViewBinding(inflater: LayoutInflater): FragmentPersonalBinding {
        return FragmentPersonalBinding.inflate(inflater)
    }
}