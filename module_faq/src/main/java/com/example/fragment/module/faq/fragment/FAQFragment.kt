package com.example.fragment.module.faq.fragment

import android.view.LayoutInflater
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.faq.databinding.FragmentFaqBinding

class FAQFragment : ViewModelFragment<FragmentFaqBinding, BaseViewModel>() {

    override fun setViewBinding(inflater: LayoutInflater): FragmentFaqBinding {
        return FragmentFaqBinding.inflate(inflater)
    }
}