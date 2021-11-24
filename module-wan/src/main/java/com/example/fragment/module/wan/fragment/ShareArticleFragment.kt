package com.example.fragment.module.wan.fragment

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
import com.example.fragment.module.wan.databinding.FragmentShareArticleBinding
import com.example.fragment.module.wan.model.ShareArticleModel

class ShareArticleFragment : RouterFragment() {

    private val viewModel: ShareArticleModel by viewModels()
    private var _binding: FragmentShareArticleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShareArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.browser.setOnClickListener {
            val link = binding.link.text.toString()
            if (checkParameter(link)) {
                val args = bundleOf(Keys.URL to link)
                activity.navigation(Router.WEB, args)
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
            when (result.errorCode) {
                "0" -> activity.onBackPressed()
                else -> activity.showTips(result.errorMsg)
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