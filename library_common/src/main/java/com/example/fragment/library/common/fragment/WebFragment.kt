package com.example.fragment.library.common.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.base.component.activity.OnBackPressedListener
import com.example.fragment.library.base.utils.WebHelper
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.databinding.FragmentWebBinding
import com.example.fragment.library.common.model.BaseViewModel
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient

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

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view.evaluateJavascript(injectVConsoleJs()) {}
                view.evaluateJavascript(newVConsoleJs()) {}
                binding.snailBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                binding.snailBar.visibility = View.GONE
            }
        }
        webHelper.webView.webChromeClient = object : WebChromeClient() {

            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
                binding.title.text = title
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.snailBar.progress = newProgress
            }
        }
        webHelper.webView.loadUrl(url)
    }

    private fun injectVConsoleJs(): String? {
        return try {
            resources.assets.open("js/vconsole.min.js").use {
                val buffer = ByteArray(it.available())
                it.read(buffer)
                String(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun newVConsoleJs(): String {
        return """
                    var vConsole = new VConsole();
                    console.log('Hello world');
        """.trimIndent()
    }
}