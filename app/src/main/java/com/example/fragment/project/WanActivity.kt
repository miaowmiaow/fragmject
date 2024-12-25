package com.example.fragment.project

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fragment.project.data.User
import com.example.fragment.project.ui.web.WebViewManager
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.launch

class WanActivity : ComponentActivity() {

    private var data by mutableStateOf<Uri?>(null)
    private var user by mutableStateOf<User?>(null)
    private var exitTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WanHelper.getUser().collect {
                    user = it
                }
            }
        }
        setContent {
            val darkTheme = user?.darkTheme ?: false
            // 设置状态栏为亮色模式，字体变为深色
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = !darkTheme //设置导航栏亮起
            WanTheme(darkTheme) {
                WanNavGraph(parseScheme(data ?: intent.data), user)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        //WebView预创建
        WebViewManager.prepare(applicationContext)
        //双击返回键回退桌面
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - exitTime > 2000) {
                    exitTime = System.currentTimeMillis()
                    val msg = getString(R.string.one_more_press_2_back)
                    Toast.makeText(this@WanActivity, msg, Toast.LENGTH_SHORT).show()
                } else {
                    moveTaskToBack(true)
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        data = intent.data
    }

    override fun onDestroy() {
        super.onDestroy()
        WanHelper.close()
        WebViewManager.destroy()
    }

    /**
     * 解析 url scheme（如果有的话）导航到指定页面
     * path 为指定页面导航 route，详情参考 WanNavGraph，示例如下：
     * wan://com.fragment.project/rank_route
     * wan://com.fragment.project/search_route/动画
     * wan://com.fragment.project/web_route/https://wanandroid.com
     */
    private fun parseScheme(uri: Uri?): String? {
        return when {
            uri != null && uri.scheme == "wan" && uri.host == "com.fragment.project" ->
                uri.path?.substring(1)

            else -> null
        }
    }

}