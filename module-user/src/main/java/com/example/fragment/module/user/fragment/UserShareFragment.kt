package com.example.fragment.module.user.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.databinding.UserShareFragmentBinding
import com.example.fragment.module.user.model.UserShareModel

class UserShareFragment : RouterFragment() {

    private val viewModel: UserShareModel by viewModels()
    private var _binding: UserShareFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserShareFragmentBinding.inflate(inflater, container, false)
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
        binding.browser.setOnClickListener {
            val url = Uri.encode(binding.link.text.toString())
            if (checkParameter(url)) {
                activity.navigation(Router.WEB, bundleOf(Keys.URL to url))
            }
        }
        binding.share.setOnClickListener {
            val title = binding.title.text.toString()
            val link = binding.link.text.toString()
            if (checkParameter(link)) {
                viewModel.getShareArticle(title, link)
            }
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.shareArticleResult.observe(viewLifecycleOwner) { result ->
            wanSuccessCallback(result) {
                activity.onBackPressed()
            }
        }
        return viewModel
    }

    override fun initLoad() {}

    private fun checkParameter(link: String): Boolean {
        if (link.isBlank()) {
            activity.showTips("分享链接不能为空")
            return false
        }
        return true
    }

}