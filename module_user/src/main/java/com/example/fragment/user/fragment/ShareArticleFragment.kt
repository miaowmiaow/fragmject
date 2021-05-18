package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.user.databinding.FragmentShareArticleBinding
import com.example.fragment.user.model.UserViewModel

class ShareArticleFragment : ViewModelFragment<FragmentShareArticleBinding, UserViewModel>() {

    override fun setViewBinding(inflater: LayoutInflater): FragmentShareArticleBinding {
        return FragmentShareArticleBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.black.setOnClickListener { baseActivity.onBackPressed() }
        binding.browser.setOnClickListener {
            val link = binding.link.text.toString()
            if (checkParameter(link)) {
                val args = Bundle()
                args.putString(Keys.URL, link)
                baseActivity.navigation(Router.WEB, args)
            }
        }
        binding.share.setOnClickListener {
            val title = binding.title.text.toString()
            val link = binding.link.text.toString()
            if (checkParameter(link)) {
                viewModel.shareArticle(title, link)
            }
        }
    }

    private fun update() {
        viewModel.shareArticleResult.observe(viewLifecycleOwner, {
            when (it.errorCode) {
                "0" -> {
                    baseActivity.onBackPressed()
                }
                "-1001" -> {
                    baseActivity.navigation(Router.LOGIN)
                }
            }
            baseActivity.showTips(it.errorMsg)
        })
    }

    private fun checkParameter(link: String): Boolean {
        if (link.isBlank()) {
            baseActivity.showTips("分享链接不能为空")
            return false
        }
        return true
    }

}