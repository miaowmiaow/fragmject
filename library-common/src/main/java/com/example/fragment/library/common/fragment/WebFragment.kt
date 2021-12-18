package com.example.fragment.library.common.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.WebViewHelper
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.databinding.FragmentWebBinding
import com.tencent.smtt.sdk.WebView

class WebFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): WebFragment {
            return WebFragment()
        }
    }

    private var _binding: FragmentWebBinding? = null
    private val binding get() = _binding!!

    private lateinit var webViewHelper: WebViewHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        webViewHelper.onResume()
        super.onResume()
    }

    override fun onPause() {
        webViewHelper.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webViewHelper.onDestroyView()
        _binding = null
    }

    override fun initView() {
        webViewHelper = WebViewHelper.with(binding.webContainer).injectVConsole(false)
        webViewHelper.onPageChangedListener = object : WebViewHelper.OnPageChangedListener {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
            }
        }
        val url = requireArguments().getString(Keys.URL)
        if (!url.isNullOrBlank()) {
            webViewHelper.loadUrl(Uri.decode(url))
        }
        binding.statusBar.setStatusBarTheme(true)
        val onBackPressed = activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!webViewHelper.canGoBack()) {
                this.isEnabled = false
                activity.onBackPressed()
            }
        }
        binding.black.setOnClickListener {
            if (!webViewHelper.canGoBack()) {
                onBackPressed.isEnabled = false
                activity.onBackPressed()
            }
        }
        binding.forward.setOnClickListener {
            webViewHelper.canGoForward()
        }
        binding.refresh.setOnClickListener {
            webViewHelper.reload()
        }
        binding.browse.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                activity.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    override fun initLoad() {}

}