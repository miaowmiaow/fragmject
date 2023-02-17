package com.example.fragment.project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.utils.WebViewManager
import com.example.fragment.project.utils.WanHelper

class WanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(ComposeView(this).apply {
            setContent {
                WanTheme {
                    WanNavGraph()
                }
            }
        })
        initWebView();
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }

    private fun initWebView() {
        //WebView预加载
        WebViewManager.prepare(applicationContext)
    }

}