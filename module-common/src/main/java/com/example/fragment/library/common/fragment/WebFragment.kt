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
import com.example.fragment.library.base.utils.WebViewManager
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.databinding.WebFragmentBinding
import com.tencent.smtt.sdk.WebView

class WebFragment : RouterFragment() {

    private var _binding: WebFragmentBinding? = null
    private val binding get() = _binding!!
    private var _webViewHelper: WebViewHelper? = null
    private val webViewHelper get() = _webViewHelper!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = WebFragmentBinding.inflate(inflater, container, false)
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
        _webViewHelper = null
        _binding = null
    }

    override fun initView() {
        val url = Uri.decode(requireArguments().getString(Keys.URL))
        val webView = WebViewManager.obtain(requireActivity())
        binding.webContainer.addView(
            webView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        _webViewHelper = WebViewHelper(webView).apply {
            setOnPageChangedListener(object : WebViewHelper.OnPageChangedListener {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.progressBar.visibility = View.GONE
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                }
            })
            loadUrl(url)
        }
        val callback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (!webViewHelper.canGoBack()) {
                    this.isEnabled = false
                    onBackPressed()
                }
            }
        binding.black.setOnClickListener {
            if (!webViewHelper.canGoBack()) {
                callback.isEnabled = false
                onBackPressed()
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
                requireActivity().startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun initViewModel(): BaseViewModel? {
        return null
    }

}