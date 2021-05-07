package com.example.fragment.module.system.fragment

import android.view.LayoutInflater
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.system.databinding.FragmentSystemBinding

class SystemFragment : ViewModelFragment<FragmentSystemBinding, BaseViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): SystemFragment {
            return SystemFragment()
        }
    }

    override fun setViewBinding(inflater: LayoutInflater): FragmentSystemBinding {
        return FragmentSystemBinding.inflate(inflater)
    }
}