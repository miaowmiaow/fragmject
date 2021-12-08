package com.example.fragment.library.common.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.WebHelper
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

    private lateinit var webHelper: WebHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        webHelper.onPause()
        super.onPause()
    }

    override fun onResume() {
        webHelper.onResume()
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webHelper.onDestroy()
        _binding = null
    }

    override fun initView() {
        val onBackPressedCallback =
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (webHelper.getWebView().canGoBack()) {
                    webHelper.getWebView().goBack()
                } else {
                    this.isEnabled = false
                    activity.onBackPressed()
                }
            }
        binding.black.setOnClickListener {
            if (webHelper.getWebView().canGoBack()) {
                webHelper.getWebView().goBack()
            } else {
                onBackPressedCallback.isEnabled = false
                activity.onBackPressed()
            }
        }
        webHelper = WebHelper.with(binding.webContainer)
        webHelper.onReceivedTitleListener = object : WebHelper.OnReceivedTitleListener {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                binding.title.text = title
            }
        }
        val url = requireArguments().getString(Keys.URL)
        if (!url.isNullOrBlank()) {
            webHelper.loadUrl(Uri.decode(url))
        }
    }

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    override fun initLoad() {}

}