package com.example.fragment.library.common.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import com.example.fragment.library.base.component.activity.OnBackPressedListener
import com.example.fragment.library.common.constant.Argument
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.databinding.FragmentWebBinding
import com.example.fragment.library.common.model.BaseViewModel
import com.tencent.smtt.export.external.interfaces.IX5WebSettings
import com.tencent.smtt.sdk.*

class WebFragment : ViewModelFragment<FragmentWebBinding, BaseViewModel>(),
    OnBackPressedListener {

    companion object {
        @JvmStatic
        fun newInstance(): WebFragment {
            return WebFragment()
        }
    }

    private var url = "https://wanandroid.com/"

    override fun setViewBinding(inflater: LayoutInflater): FragmentWebBinding {
        return FragmentWebBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            url = this.getString(Argument.URL).toString()
        }
        setupView()
        webViewSetting()
    }

    override fun onResume() {
        super.onResume()
        getRouterActivity().registerOnBackPressedListener(this::class.java.simpleName, this)
    }

    override fun onPause() {
        super.onPause()
        getRouterActivity().removerOnBackPressedListener(this::class.java.simpleName)
    }

    override fun onBackPressed(): Boolean {
        return if (binding.webView.canGoBack()) {
            binding.webView.goBack()
            true
        } else {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webView.destroy()
    }

    private fun setupView() {
        binding.black.setOnClickListener {
            getRouterActivity().onBackPressed()
        }
        binding.search.setOnClickListener {
            getRouterActivity().navigation(Router.SEARCH)
        }
        binding.publish.setOnClickListener {
            getRouterActivity().navigation(Router.PUBLISH)
        }
    }

    private fun webViewSetting() {
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {

            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
                binding.title.text = title
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {

            }
        }
        val webSetting = binding.webView.settings
        webSetting.allowFileAccess = true
        webSetting.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        webSetting.setSupportZoom(true)
        webSetting.builtInZoomControls = true
        webSetting.useWideViewPort = true
        webSetting.setSupportMultipleWindows(false)
        webSetting.setAppCacheEnabled(true)
        webSetting.domStorageEnabled = true
        webSetting.javaScriptEnabled = true
        webSetting.setGeolocationEnabled(true)
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE)
        webSetting.setAppCachePath(fragmentActivity.getDir("appcache", 0).path)
        webSetting.databasePath = fragmentActivity.getDir("databases", 0).path
        webSetting.setGeolocationDatabasePath(fragmentActivity.getDir("geolocation", 0).path)
        webSetting.pluginState = WebSettings.PluginState.ON_DEMAND
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSetting.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
            CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
        }
        binding.webView.settingsExtension?.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY)
        binding.webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        binding.webView.loadUrl(url)
    }
}