package com.example.fragment.project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.ComposeView
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.dialog.StandardDialog
import com.example.fragment.library.base.utils.WebViewManager
import com.example.fragment.project.utils.WanHelper

class WanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        WanHelper.privacyAgreement({ initContentView() }, {
            window.setBackgroundDrawableResource(R.drawable.bg)
            val listener = object : StandardDialog.OnDialogClickListener {
                override fun onConfirm(dialog: StandardDialog) {
                    WanHelper.allowPrivacyAgreement()
                    initContentView()
                }

                override fun onCancel(dialog: StandardDialog) {
                    WanHelper.denyPrivacyAgreement()
                    finish()
                }
            }
            StandardDialog.newInstance()
                .setTitle(getString(R.string.privacy_agreement_title))
                .setContent(getString(R.string.privacy_agreement_content))
                .setOnDialogClickListener(listener)
                .show(supportFragmentManager)
        })
        initWebView()
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }

    private fun initContentView() {
        setContentView(ComposeView(this).apply {
            setContent {
                WanTheme {
                    WanNavGraph()
                }
            }
        })
        WanHelper.getUiMode { mode ->
            when (mode) {
                "1" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                "2" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun initWebView() {
        //WebView预加载
        WebViewManager.prepare(applicationContext)
    }

}