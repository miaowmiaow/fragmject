package com.example.fragment.project

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.fragment.project.ui.web.WebViewManager
import com.example.miaow.base.debug.DebugBridge

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
        // 仅在 Debug 构建中启用 WebView 调试，避免在 Release 包暴露调试接口
        if (DebugBridge.allowWebContentsDebugging) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 数据库随进程生命周期管理，无需在 Activity 销毁时关闭
        WebViewManager.destroy()
    }
}