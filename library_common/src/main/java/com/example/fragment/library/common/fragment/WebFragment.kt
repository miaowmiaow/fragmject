package com.example.fragment.library.common.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.base.component.activity.OnBackPressedListener
import com.example.fragment.library.base.utils.WebHelper
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.databinding.FragmentWebBinding
import com.example.fragment.library.common.model.BaseViewModel
import com.tencent.smtt.sdk.*

class WebFragment : ViewModelFragment<FragmentWebBinding, BaseViewModel>(),
    OnBackPressedListener {

    companion object {
        @JvmStatic
        fun newInstance(): WebFragment {
            return WebFragment()
        }
    }

    private lateinit var webHelper: WebHelper
    private var url = "https://wanandroid.com/"

    override fun setViewBinding(inflater: LayoutInflater): FragmentWebBinding {
        return FragmentWebBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            url = this.getString(Keys.URL).toString()
        }
        setupView()
        webViewSetting()
    }

    override fun onResume() {
        super.onResume()
        baseActivity.registerOnBackPressedListener(this::class.java.simpleName, this)
    }

    override fun onPause() {
        super.onPause()
        baseActivity.removerOnBackPressedListener(this::class.java.simpleName)
    }

    override fun onBackPressed(): Boolean {
        return if (webHelper.webView.canGoBack()) {
            webHelper.webView.goBack()
            true
        } else {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webHelper.onDestroy()
    }

    private fun setupView() {
        binding.black.setOnClickListener {
            baseActivity.onBackPressed()
        }
        binding.collect.setOnClickListener {

        }
        binding.more.setOnClickListener {

        }
    }

    private fun webViewSetting() {
        webHelper = WebHelper.with(binding.webContainer)
        webHelper.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }
        webHelper.webView.webChromeClient = object : WebChromeClient() {

            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
                binding.title.text = title
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {

            }
        }
        webHelper.webView.loadUrl(url)
    }
}