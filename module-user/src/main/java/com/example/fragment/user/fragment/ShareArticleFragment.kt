package com.example.fragment.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentShareArticleBinding
import com.example.fragment.user.model.UserViewModel

class ShareArticleFragment : RouterFragment() {

    private val viewModel: UserViewModel by viewModels()
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
        viewModel.shareArticleResult.observe(viewLifecycleOwner, { result ->
            when (result.errorCode) {
                "0" -> {
                    baseActivity.onBackPressed()
                }
                "-1001" -> {
                    WanHelper.setUser(UserBean())
                    baseActivity.navigation(Router.LOGIN)
                }
            }
            if (result.errorCode.isNotBlank() && result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
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