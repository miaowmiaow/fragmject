package com.example.fragment.module.setup.fragment

import android.view.LayoutInflater
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.setup.databinding.FragmentSetupBinding

class SetupFragment : ViewModelFragment<FragmentSetupBinding, BaseViewModel>() {

    override fun setViewBinding(inflater: LayoutInflater): FragmentSetupBinding {
        return FragmentSetupBinding.inflate(inflater)
    }
}