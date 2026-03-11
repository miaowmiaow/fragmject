package com.example.fragment.project

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.fragment.project.ui.web.WebViewManager
import com.example.fragment.project.utils.WanHelper

class WanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WanTheme(window) {
                WanNavGraph()
            }
        }
        // WebView 预创建
        WebViewManager.prepare(applicationContext)
        //启用 WebView 调试
        WebView.setWebContentsDebuggingEnabled(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }
}